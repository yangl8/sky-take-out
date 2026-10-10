package com.sky.mapper;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInterceptor;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在独立 MySQL 数据库中记录用户端订单分页的优化前基准
 */
class OrderPaginationBaselineMySqlTest {

    private static final String JDBC_URL_ENV = "SKY_ORDER_PAGE_DB_URL";
    private static final String JDBC_USER_ENV = "SKY_ORDER_PAGE_DB_USER";
    private static final String JDBC_PASSWORD_ENV = "SKY_ORDER_PAGE_DB_PASSWORD";
    private static final int EXPECTED_ORDER_COUNT = 10000;
    private static final int EXPECTED_DETAIL_COUNT = 30000;
    private static final int BASELINE_WARMUP_ROUNDS = 5;
    private static final int BASELINE_MEASURED_ROUNDS = 40;
    private static final int INTERLEAVED_WARMUP_ROUNDS = 30;
    private static final int INTERLEAVED_MEASURED_ROUNDS = 100;
    private static final int[] PAGE_SIZES = {10, 20, 50};
    private static final long USER_ID = 9001L;

    /**
     * 测量 10、20、50 条分页并将完整环境和结果写入 JSON
     *
     * @throws Exception 数据库连接、SQL 执行或基准文件写入失败
     */
    @Test
    void recordsUserOrderPaginationBaseline() throws Exception {
        LogFactory.useNoLogging();
        String mode = System.getProperty("order.pagination.mode", "interleaved");
        assertTrue("baseline".equals(mode) || "interleaved".equals(mode), "未知订单分页基准模式");
        boolean interleaved = "interleaved".equals(mode);
        boolean preservePreviousReport = Boolean.parseBoolean(
                System.getProperty("order.pagination.baseline.preservePrevious", "true"));
        Path outputPath = Paths.get(System.getProperty("order.pagination.baseline.output",
                "target/order-pagination-baseline.json"));
        String previousReport = "";
        if (interleaved && preservePreviousReport && Files.exists(outputPath)) {
            previousReport = new String(Files.readAllBytes(outputPath), StandardCharsets.UTF_8);
            assertTrue(previousReport.trim().startsWith("{") && previousReport.trim().endsWith("}"),
                    "已有基准 JSON 格式无效");
        }
        String jdbcUrl = System.getenv(JDBC_URL_ENV);
        String username = System.getenv(JDBC_USER_ENV);
        String password = System.getenv(JDBC_PASSWORD_ENV);
        Assumptions.assumeTrue(jdbcUrl != null && username != null && password != null,
                "设置独立 sky_take_out_perf_test 数据库连接变量后运行订单分页基准");
        assertSafeDatabaseUrl(jdbcUrl);

        SqlCounter sqlCounter = new SqlCounter();
        SqlSessionFactory sessionFactory = createSessionFactory(jdbcUrl, username, password, sqlCounter);
        String databaseVersion;
        long orderCount;
        long detailCount;
        try (SqlSession session = sessionFactory.openSession()) {
            try (Statement statement = session.getConnection().createStatement();
                 ResultSet resultSet = statement.executeQuery("select version(), (select count(*) from orders), "
                         + "(select count(*) from order_detail)")) {
                assertTrue(resultSet.next());
                databaseVersion = resultSet.getString(1);
                orderCount = resultSet.getLong(2);
                detailCount = resultSet.getLong(3);
            }
        }
        assertEquals(EXPECTED_ORDER_COUNT, orderCount, "请先运行数据生成脚本，避免在测量时生成数据");
        assertEquals(EXPECTED_DETAIL_COUNT, detailCount, "请先运行数据生成脚本，避免在测量时生成数据");

        int warmupRounds = interleaved ? INTERLEAVED_WARMUP_ROUNDS : BASELINE_WARMUP_ROUNDS;
        int measuredRounds = interleaved ? INTERLEAVED_MEASURED_ROUNDS : BASELINE_MEASURED_ROUNDS;
        List<PageMeasurement> measurements = interleaved
                ? measureInterleaved(sessionFactory, sqlCounter, warmupRounds, measuredRounds)
                : measureSequential(sessionFactory, sqlCounter, warmupRounds, measuredRounds);
        verifySecondPageBatchQuery(sessionFactory, sqlCounter);
        String explainPlan = explainQueryPlans(jdbcUrl, username, password);
        String report = buildReport(databaseVersion, orderCount, detailCount, explainPlan, measurements,
                warmupRounds, measuredRounds, interleaved);
        if (interleaved && preservePreviousReport) {
            report = "{\n  \"finalBaselineRun\": \"interleavedFollowup\",\n"
                    + "  \"previousBaseline\": " + (previousReport.isEmpty() ? "null" : previousReport) + ",\n"
                    + "  \"interleavedFollowup\": " + report + "\n}\n";
        }
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, report.getBytes(StandardCharsets.UTF_8));
        System.out.println("订单分页基准已写入 " + outputPath.toAbsolutePath());
        System.out.println(report);
    }

    /**
     * 使用真实 PageHelper 验证第2页批量明细查询只读取当前页订单
     *
     * @param sessionFactory 加载真实订单 Mapper 的会话工厂
     * @param sqlCounter 记录 MyBatis 实际 Mapper 查询的计数器
     */
    private void verifySecondPageBatchQuery(SqlSessionFactory sessionFactory, SqlCounter sqlCounter) {
        Set<Long> firstPageOrderIds = new HashSet<>();
        LocalDateTime firstPageLastOrderTime;
        try (SqlSession session = sessionFactory.openSession()) {
            OrdersPageQueryDTO query = new OrdersPageQueryDTO();
            query.setUserId(USER_ID);

            sqlCounter.reset();
            PageHelper.startPage(1, 10);
            Page<Orders> firstPage = session.getMapper(OrderMapper.class).pageQuery(query);
            assertEquals(10, firstPage.size());
            assertEquals(EXPECTED_ORDER_COUNT, firstPage.getTotal());
            assertNull(PageHelper.getLocalPage(), "订单分页结束后 PageHelper 状态应已清理");
            for (Orders order : firstPage) {
                firstPageOrderIds.add(order.getId());
            }
            firstPageLastOrderTime = firstPage.get(firstPage.size() - 1).getOrderTime();
            List<OrderDetail> firstPageDetails = session.getMapper(OrderDetailMapper.class)
                    .getByOrderIds(new ArrayList<>(firstPageOrderIds));
            assertEquals(30, firstPageDetails.size());
            assertEquals(3, sqlCounter.totalSqlCount, "首页 SQL 应包含 COUNT、分页和批量明细查询");
        }

        try (SqlSession session = sessionFactory.openSession()) {
            OrdersPageQueryDTO query = new OrdersPageQueryDTO();
            query.setUserId(USER_ID);
            sqlCounter.reset();
            PageHelper.startPage(2, 10);
            Page<Orders> secondPage = session.getMapper(OrderMapper.class).pageQuery(query);
            assertEquals(10, secondPage.size());
            assertEquals(EXPECTED_ORDER_COUNT, secondPage.getTotal());
            assertNull(PageHelper.getLocalPage(), "第2页订单查询结束后 PageHelper 状态应已清理");
            assertTrue(!secondPage.get(0).getOrderTime().isAfter(firstPageLastOrderTime),
                    "第2页订单时间应排在首页之后");
            for (int index = 1; index < secondPage.size(); index++) {
                assertTrue(!secondPage.get(index).getOrderTime().isAfter(secondPage.get(index - 1).getOrderTime()),
                        "第2页应保持下单时间降序");
            }
            List<Long> secondPageOrderIds = new ArrayList<>();
            for (Orders order : secondPage) {
                secondPageOrderIds.add(order.getId());
            }
            assertTrue(Collections.disjoint(firstPageOrderIds, secondPageOrderIds),
                    "第2页不能包含首页订单");

            List<OrderDetail> secondPageDetails = session.getMapper(OrderDetailMapper.class)
                    .getByOrderIds(secondPageOrderIds);
            assertEquals(30, secondPageDetails.size());
            Map<Long, Integer> detailsByOrderId = new HashMap<>();
            Set<Long> detailIds = new HashSet<>();
            for (OrderDetail detail : secondPageDetails) {
                assertTrue(secondPageOrderIds.contains(detail.getOrderId()), "批量查询不能返回其他页的明细");
                assertTrue(detailIds.add(detail.getId()), "批量查询不能重复返回订单明细");
                detailsByOrderId.put(detail.getOrderId(), detailsByOrderId.getOrDefault(detail.getOrderId(), 0) + 1);
            }
            assertEquals(30, detailIds.size());
            for (Long orderId : secondPageOrderIds) {
                assertEquals(3, detailsByOrderId.get(orderId), "第2页订单明细不能遗漏或重复");
            }
            assertEquals(3, sqlCounter.totalSqlCount, "第2页 SQL 应包含 COUNT、分页和批量明细查询");
            assertEquals(1, sqlCounter.countQueryCount);
            assertEquals(1, sqlCounter.orderPageQueryCount);
            assertEquals(1, sqlCounter.detailQueryCount);
        }
    }

    /**
     * 按固定分页大小顺序执行预热和正式采样
     *
     * @param sessionFactory 加载真实订单 Mapper 的会话工厂
     * @param sqlCounter 记录 MyBatis 实际 Mapper 查询的计数器
     * @param warmupRounds 每种分页大小的预热次数
     * @param measuredRounds 每种分页大小的正式采样次数
     * @return 按 10、20、50 顺序测得的结果
     */
    private List<PageMeasurement> measureSequential(SqlSessionFactory sessionFactory, SqlCounter sqlCounter,
                                                    int warmupRounds, int measuredRounds) {
        List<PageMeasurement> measurements = new ArrayList<>();
        for (int pageSize : PAGE_SIZES) {
            for (int round = 0; round < warmupRounds; round++) {
                runPageQuery(sessionFactory, sqlCounter, pageSize);
            }
            List<Long> latencySamples = new ArrayList<>();
            for (int round = 0; round < measuredRounds; round++) {
                long startNanos = System.nanoTime();
                runPageQuery(sessionFactory, sqlCounter, pageSize);
                latencySamples.add(System.nanoTime() - startNanos);
            }
            measurements.add(createMeasurement(pageSize, latencySamples, false));
        }
        return measurements;
    }

    /**
     * 轮换分页顺序完成充分预热和正式采样，保留各分页样本的实际观测顺序
     *
     * @param sessionFactory 加载真实订单 Mapper 的会话工厂
     * @param sqlCounter 记录 MyBatis 实际 Mapper 查询的计数器
     * @param warmupRounds 每种分页大小的预热次数
     * @param measuredRounds 每种分页大小的正式采样次数
     * @return 按分页大小汇总的交错采样结果
     */
    private List<PageMeasurement> measureInterleaved(SqlSessionFactory sessionFactory, SqlCounter sqlCounter,
                                                     int warmupRounds, int measuredRounds) {
        for (int round = 0; round < warmupRounds; round++) {
            for (int pageSize : rotatedPageSizes(round)) {
                runPageQuery(sessionFactory, sqlCounter, pageSize);
            }
        }
        List<List<Long>> samplesByPage = new ArrayList<>();
        for (int ignored : PAGE_SIZES) {
            samplesByPage.add(new ArrayList<>());
        }
        for (int round = 0; round < measuredRounds; round++) {
            for (int pageSize : rotatedPageSizes(round)) {
                long startNanos = System.nanoTime();
                runPageQuery(sessionFactory, sqlCounter, pageSize);
                samplesByPage.get(pageSizeIndex(pageSize)).add(System.nanoTime() - startNanos);
            }
        }
        List<PageMeasurement> measurements = new ArrayList<>();
        for (int index = 0; index < PAGE_SIZES.length; index++) {
            measurements.add(createMeasurement(PAGE_SIZES[index], samplesByPage.get(index), true));
        }
        return measurements;
    }

    /**
     * 按轮次循环平移 10、20、50 的分页顺序
     *
     * @param round 当前预热或采样轮次
     * @return 本轮交错执行的分页大小顺序
     */
    private int[] rotatedPageSizes(int round) {
        int offset = round % PAGE_SIZES.length;
        return new int[]{PAGE_SIZES[offset], PAGE_SIZES[(offset + 1) % PAGE_SIZES.length],
                PAGE_SIZES[(offset + 2) % PAGE_SIZES.length]};
    }

    /**
     * 返回分页大小在固定结果数组中的位置
     *
     * @param pageSize 每页订单数
     * @return 对应的数组下标
     */
    private int pageSizeIndex(int pageSize) {
        for (int index = 0; index < PAGE_SIZES.length; index++) {
            if (PAGE_SIZES[index] == pageSize) {
                return index;
            }
        }
        throw new IllegalArgumentException("未配置的分页大小: " + pageSize);
    }

    /**
     * 根据正式样本计算中位数和 nearest-rank P95，并保存完整延迟样本
     *
     * @param pageSize 每页订单数
     * @param latencySamplesNanos 延迟纳秒样本
     * @param preserveObservationOrder 是否保留原始观测顺序
     * @return 延迟分位数与 SQL 次数统计
     */
    private PageMeasurement createMeasurement(int pageSize, List<Long> latencySamplesNanos,
                                              boolean preserveObservationOrder) {
        assertEquals("interleaved".equals(System.getProperty("order.pagination.mode", "interleaved")),
                preserveObservationOrder);
        List<Long> sortedSamples = new ArrayList<>(latencySamplesNanos);
        Collections.sort(sortedSamples);
        long p95Nanos = sortedSamples.get((int) Math.ceil(sortedSamples.size() * 0.95) - 1);
        int upperMedianIndex = sortedSamples.size() / 2;
        double medianNanos = (sortedSamples.get(upperMedianIndex - 1)
                + sortedSamples.get(upperMedianIndex)) / 2.0;
        List<Double> samplesMs = new ArrayList<>();
        List<Long> reportedSamples = preserveObservationOrder ? latencySamplesNanos : sortedSamples;
        for (Long latencySample : reportedSamples) {
            samplesMs.add(latencySample / 1_000_000.0);
        }
        int returnedOrders = pageSize;
        return new PageMeasurement(pageSize, returnedOrders, 3, 1,
                medianNanos / 1_000_000.0, p95Nanos / 1_000_000.0, samplesMs);
    }

    /**
     * 按用户端分页服务的真实 Mapper 顺序执行订单页查询和逐单明细查询
     *
     * @param sessionFactory 加载真实订单 Mapper 的会话工厂
     * @param sqlCounter 记录 Mapper 查询的计数器
     * @param pageSize 每页订单数
     * @return 实际返回订单数及拆分后的查询次数
     */
    private QueryResult runPageQuery(SqlSessionFactory sessionFactory, SqlCounter sqlCounter, int pageSize) {
        sqlCounter.reset();
        try (SqlSession session = sessionFactory.openSession()) {
            OrdersPageQueryDTO query = new OrdersPageQueryDTO();
            query.setPage(1);
            query.setPageSize(pageSize);
            query.setUserId(USER_ID);
            PageHelper.startPage(1, pageSize);
            Page<Orders> page = session.getMapper(OrderMapper.class).pageQuery(query);
            List<Long> orderIds = new ArrayList<>();
            for (Orders order : page) {
                orderIds.add(order.getId());
            }
            List<OrderDetail> details = session.getMapper(OrderDetailMapper.class).getByOrderIds(orderIds);
            Map<Long, Integer> detailsByOrderId = new HashMap<>();
            for (OrderDetail detail : details) {
                detailsByOrderId.put(detail.getOrderId(), detailsByOrderId.getOrDefault(detail.getOrderId(), 0) + 1);
            }
            int returnedOrders = page.size();
            assertEquals(pageSize, returnedOrders);
            assertEquals(EXPECTED_ORDER_COUNT, page.getTotal());
            assertEquals(2, sqlCounter.mapperQueryCount,
                    "Mapper 层应执行一次订单分页 SQL 和一次批量明细 SQL");
            assertEquals(1, sqlCounter.countQueryCount, "总 SQL 数必须包含 PageHelper 实际执行的 COUNT");
            assertEquals(1, sqlCounter.orderPageQueryCount);
            assertEquals(1, sqlCounter.detailQueryCount);
            assertEquals(3, sqlCounter.totalSqlCount);
            assertEquals(pageSize * 3, details.size());
            for (Long orderId : orderIds) {
                assertEquals(3, detailsByOrderId.get(orderId));
            }
            return new QueryResult(returnedOrders, sqlCounter.detailQueryCount, sqlCounter.totalSqlCount);
        }
    }

    /**
     * 创建仅加载订单 Mapper XML 和 MySQL PageHelper 的测试会话工厂
     *
     * @param jdbcUrl 独立性能数据库 JDBC 地址
     * @param username 数据库用户名
     * @param password 数据库密码
     * @param sqlCounter Mapper 查询计数器
     * @return 可执行项目订单 Mapper 的 MyBatis 会话工厂
     * @throws Exception Mapper XML 加载失败
     */
    private SqlSessionFactory createSessionFactory(String jdbcUrl, String username, String password,
                                                   SqlCounter sqlCounter) throws Exception {
        PooledDataSource dataSource = new PooledDataSource("com.mysql.cj.jdbc.Driver", jdbcUrl, username, password);
        Environment environment = new Environment("order-pagination-baseline", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setLogImpl(NoLoggingImpl.class);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.getTypeAliasRegistry().registerAlias(Orders.class);
        configuration.getTypeAliasRegistry().registerAlias(OrderDetail.class);
        PageInterceptor pageInterceptor = new PageInterceptor();
        Properties pageProperties = new Properties();
        pageProperties.setProperty("helperDialect", "mysql");
        pageProperties.setProperty("reasonable", "false");
        pageInterceptor.setProperties(pageProperties);
        configuration.addInterceptor(pageInterceptor);
        configuration.addInterceptor(sqlCounter);
        loadMapperXml(configuration, "mapper/OrderMapper.xml");
        loadMapperXml(configuration, "mapper/OrderDetailMapper.xml");
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    /**
     * 将项目 Mapper XML 注册到基准测试的 MyBatis 配置
     *
     * @param configuration 基准测试 MyBatis 配置
     * @param resource Mapper XML 类路径
     * @throws Exception Mapper XML 不存在或解析失败
     */
    private void loadMapperXml(Configuration configuration, String resource) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertTrue(inputStream != null, "找不到 Mapper XML: " + resource);
            new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    /**
     * 使用与基准查询相同的筛选和排序条件收集 MySQL 执行计划
     *
     * @param jdbcUrl 独立性能数据库 JDBC 地址
     * @param username 数据库用户名
     * @param password 数据库密码
     * @return 订单 COUNT、分页和当前页批量明细查询的 EXPLAIN 输出
     * @throws Exception 数据库连接或 EXPLAIN 执行失败
     */
    private String explainQueryPlans(String jdbcUrl, String username, String password) throws Exception {
        StringBuilder plans = new StringBuilder();
        try (Connection connection = new PooledDataSource("com.mysql.cj.jdbc.Driver", jdbcUrl, username, password)
                .getConnection(); Statement statement = connection.createStatement()) {
            StringBuilder pageOrderIds = new StringBuilder();
            String selectPageIds = "SELECT id FROM orders WHERE user_id = 9001 AND EXISTS "
                    + "(SELECT 1 FROM order_detail od WHERE od.order_id = orders.id) "
                    + "ORDER BY order_time DESC LIMIT 0, 50";
            try (ResultSet resultSet = statement.executeQuery(selectPageIds)) {
                while (resultSet.next()) {
                    if (pageOrderIds.length() > 0) {
                        pageOrderIds.append(", ");
                    }
                    pageOrderIds.append(resultSet.getLong(1));
                }
            }
            assertTrue(pageOrderIds.length() > 0, "执行计划需要当前页订单 ID");
            String[] sqlStatements = {
                    "EXPLAIN SELECT count(0) FROM orders WHERE user_id = 9001 AND EXISTS "
                            + "(SELECT 1 FROM order_detail od WHERE od.order_id = orders.id)",
                    "EXPLAIN SELECT * FROM orders WHERE user_id = 9001 AND EXISTS "
                            + "(SELECT 1 FROM order_detail od WHERE od.order_id = orders.id) "
                            + "ORDER BY order_time DESC LIMIT 0, 10",
                    "EXPLAIN SELECT * FROM order_detail WHERE order_id IN (" + pageOrderIds
                            + ") ORDER BY order_id, id"
            };
            for (String sql : sqlStatements) {
                plans.append(sql).append('\n');
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    int columns = resultSet.getMetaData().getColumnCount();
                    while (resultSet.next()) {
                        for (int column = 1; column <= columns; column++) {
                            if (column > 1) {
                                plans.append(" | ");
                            }
                            plans.append(resultSet.getString(column));
                        }
                        plans.append('\n');
                    }
                }
            }
        }
        return plans.toString();
    }

    /**
     * 校验连接地址明确指向独立性能数据库
     *
     * @param jdbcUrl 数据库 JDBC 地址
     */
    private void assertSafeDatabaseUrl(String jdbcUrl) {
        assertTrue(jdbcUrl.matches("jdbc:mysql://[^/]+/sky_take_out_perf_test(?:\\?.*)?"),
                "基准只允许连接 sky_take_out_perf_test，禁止连接业务数据库");
    }

    /**
     * 组装可保存的基准 JSON 文档
     *
     * @param databaseVersion MySQL 版本
     * @param orderCount 订单数据规模
     * @param detailCount 订单明细数据规模
     * @param explainPlan 数据库执行计划
     * @param measurements 各分页大小的实测结果
     * @return UTF-8 JSON 内容
     */
    private String buildReport(String databaseVersion, long orderCount, long detailCount, String explainPlan,
                               List<PageMeasurement> measurements, int warmupRounds, int measuredRounds,
                               boolean interleaved) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"environment\": {\n")
                .append("    \"mysqlVersion\": \"").append(escapeJson(databaseVersion)).append("\",\n")
                .append("    \"javaVersion\": \"").append(escapeJson(System.getProperty("java.version"))).append("\",\n")
                .append("    \"os\": \"").append(escapeJson(System.getProperty("os.name"))).append("\",\n")
                .append("    \"architecture\": \"").append(escapeJson(System.getProperty("os.arch"))).append("\",\n")
                .append("    \"availableProcessors\": ").append(Runtime.getRuntime().availableProcessors()).append(",\n")
                .append("    \"capturedAt\": \"").append(Instant.now()).append("\",\n")
                .append("    \"database\": \"sky_take_out_perf_test\",\n")
                .append("    \"orders\": ").append(orderCount).append(",\n")
                .append("    \"orderDetails\": ").append(detailCount).append("\n  },\n")
                .append("  \"method\": {\"warmupsPerPageSize\": ").append(warmupRounds)
                .append(", \"samplesPerPageSize\": ").append(measuredRounds)
                .append(", \"schedule\": \"").append(interleaved ? "cyclic-interleaved" : "sequential")
                .append("\", \"orderRotation\": \"10,20,50 / 20,50,10 / 50,10,20\", ")
                .append("\"sampleOrder\": \"").append(interleaved ? "chronological" : "sorted-by-latency")
                .append("\", \"queryPath\": \"OrderMapper.pageQuery + PageHelper + OrderDetailMapper.getByOrderIds for current page\"},\n")
                .append("  \"pages\": [\n");
        for (int index = 0; index < measurements.size(); index++) {
            PageMeasurement measurement = measurements.get(index);
            json.append("    {\"pageSize\": ").append(measurement.pageSize)
                    .append(", \"returnedOrders\": ").append(measurement.returnedOrders)
                    .append(", \"totalSqlQueries\": ").append(measurement.totalSqlQueries)
                    .append(", \"countQueries\": 1, \"orderPageQueries\": 1, \"detailQueries\": ")
                    .append(measurement.detailQueries)
                    .append(", \"medianMs\": ").append(String.format(java.util.Locale.ROOT, "%.3f", measurement.medianMs))
                    .append(", \"p95Ms\": ").append(String.format(java.util.Locale.ROOT, "%.3f", measurement.p95Ms))
                    .append(", \"latencySamplesMs\": [");
            for (int sampleIndex = 0; sampleIndex < measurement.samplesMs.size(); sampleIndex++) {
                if (sampleIndex > 0) {
                    json.append(", ");
                }
                json.append(String.format(java.util.Locale.ROOT, "%.3f", measurement.samplesMs.get(sampleIndex)));
            }
            json.append("]}").append(index + 1 == measurements.size() ? "\n" : ",\n");
        }
        json.append("  ],\n  \"explain\": \"").append(escapeJson(explainPlan)).append("\"\n}\n");
        return json.toString();
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     *
     * @param value 原始字符串
     * @return 符合 JSON 字符串格式的内容
     */
    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    @Intercepts({
            @Signature(type = org.apache.ibatis.executor.Executor.class, method = "query",
                    args = {org.apache.ibatis.mapping.MappedStatement.class, Object.class, RowBounds.class,
                            org.apache.ibatis.session.ResultHandler.class}),
            @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
    })
    private static class SqlCounter implements Interceptor {
        private int mapperQueryCount;
        private int totalSqlCount;
        private int countQueryCount;
        private int orderPageQueryCount;
        private int detailQueryCount;

        /**
         * 记录订单 Mapper 发起的查询调用
         *
         * @param invocation MyBatis Executor 查询调用
         * @return MyBatis 原查询结果
         * @throws Throwable Mapper 查询失败
         */
        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            if (invocation.getTarget() instanceof StatementHandler) {
                String sql = ((StatementHandler) invocation.getTarget()).getBoundSql().getSql()
                        .replaceAll("\\s+", " ").trim().toLowerCase(java.util.Locale.ROOT);
                totalSqlCount++;
                if (sql.startsWith("select count(")) {
                    countQueryCount++;
                } else if (sql.contains("from order_detail") && sql.contains("where order_id")) {
                    detailQueryCount++;
                } else if (sql.contains("from orders")) {
                    orderPageQueryCount++;
                }
            } else {
                mapperQueryCount++;
            }
            return invocation.proceed();
        }

        /**
         * 清空当前分页测量的 Mapper 查询次数
         */
        private void reset() {
            mapperQueryCount = 0;
            totalSqlCount = 0;
            countQueryCount = 0;
            orderPageQueryCount = 0;
            detailQueryCount = 0;
        }
    }

    private static class QueryResult {
        private final int returnedOrders;
        private final int detailQueries;
        private final int totalSqlQueries;

        /**
         * 保存单次请求的返回数与 SQL 查询计数
         *
         * @param returnedOrders 实际返回的订单数
         * @param detailQueries 订单明细查询数
         * @param totalSqlQueries 包含 COUNT 的 SQL 总数
         */
        private QueryResult(int returnedOrders, int detailQueries, int totalSqlQueries) {
            this.returnedOrders = returnedOrders;
            this.detailQueries = detailQueries;
            this.totalSqlQueries = totalSqlQueries;
        }
    }

    private static class PageMeasurement {
        private final int pageSize;
        private final int returnedOrders;
        private final int totalSqlQueries;
        private final int detailQueries;
        private final double medianMs;
        private final double p95Ms;
        private final List<Double> samplesMs;

        /**
         * 保存一种分页大小的稳定查询和延迟结果
         *
         * @param pageSize 每页订单数
         * @param returnedOrders 实际返回订单数
         * @param totalSqlQueries 包含 COUNT 的 SQL 总数
         * @param detailQueries 订单明细查询数
         * @param medianMs 中位延迟毫秒数
         * @param p95Ms P95 延迟毫秒数
         * @param samplesMs 全部正式样本延迟毫秒数
         */
        private PageMeasurement(int pageSize, int returnedOrders, int totalSqlQueries, int detailQueries,
                                double medianMs, double p95Ms, List<Double> samplesMs) {
            this.pageSize = pageSize;
            this.returnedOrders = returnedOrders;
            this.totalSqlQueries = totalSqlQueries;
            this.detailQueries = detailQueries;
            this.medianMs = medianMs;
            this.p95Ms = p95Ms;
            this.samplesMs = samplesMs;
        }
    }
}
