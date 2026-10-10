package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 使用独立 MySQL 数据库验证真实订单 Mapper 的并发状态更新
 */
class OrderStatusRaceMySqlIntegrationTest {

    private static final String JDBC_URL_ENV = "SKY_ORDER_RACE_DB_URL";
    private static final String JDBC_USER_ENV = "SKY_ORDER_RACE_DB_USER";
    private static final String JDBC_PASSWORD_ENV = "SKY_ORDER_RACE_DB_PASSWORD";
    private static final String ROUNDS_ENV = "SKY_ORDER_RACE_ROUNDS";
    private static final String TEST_DATABASE = "sky_take_out_test";

    /**
     * 多轮竞争真实支付和超时取消并检查截止时间及重复更新
     *
     * @throws Exception 数据库并发测试失败
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void paymentAndTimeoutCancellationMustHaveExactlyOneWinner() throws Exception {
        String jdbcUrl = System.getenv(JDBC_URL_ENV);
        String username = System.getenv(JDBC_USER_ENV);
        String password = System.getenv(JDBC_PASSWORD_ENV);
        String roundsValue = System.getenv(ROUNDS_ENV);
        Assumptions.assumeTrue(jdbcUrl != null && username != null && password != null && roundsValue != null,
                "设置独立测试数据库连接变量后运行真实 MySQL 并发测试");
        assertTestDatabaseUrl(jdbcUrl);
        int rounds = Integer.parseInt(roundsValue);
        assertTrue(rounds == 10 || rounds == 100, "并发轮数只允许 10 轮预检或 100 轮正式验证");

        SqlSessionFactory sessionFactory = createSqlSessionFactory(jdbcUrl, username, password);
        String mysqlVersion = verifyInnoDbTestTable(jdbcUrl, username, password);
        verifyTimeoutBoundary(sessionFactory, jdbcUrl, username, password);

        String testPrefix = "order-race-" + UUID.randomUUID() + "-";
        int paymentWins = 0;
        int timeoutWins = 0;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < rounds; round++) {
                LocalDateTime timeoutCutoff = LocalDateTime.now().minusMinutes(15).truncatedTo(ChronoUnit.SECONDS);
                String orderNumber = testPrefix + round;
                long orderId = insertOverdueUnpaidOrder(jdbcUrl, username, password, orderNumber,
                        timeoutCutoff.minusSeconds(1));
                try {
                    RaceResult result = racePaymentAndTimeout(executor, sessionFactory, orderId, timeoutCutoff);
                    assertEquals(1, result.paymentRows + result.timeoutRows,
                            "每轮必须恰好有一个条件更新成功");
                    assertNotEquals(result.paymentRows, result.timeoutRows,
                            "竞争失败的一方必须影响零行");
                    assertNotEquals(result.paymentConnectionId, result.timeoutConnectionId,
                            "两个竞争线程必须使用独立 MySQL 连接");

                    Orders storedOrder = selectOrder(sessionFactory, orderId);
                    if (result.paymentRows == 1) {
                        paymentWins++;
                        assertEquals(Orders.TO_BE_CONFIRMED, storedOrder.getStatus());
                        assertEquals(Orders.PAID, storedOrder.getPayStatus());
                    } else {
                        timeoutWins++;
                        assertEquals(Orders.CANCELLED, storedOrder.getStatus());
                        assertEquals(Orders.UN_PAID, storedOrder.getPayStatus());
                    }

                    assertRepeatedTransitionsDoNotOverwrite(sessionFactory, orderId, timeoutCutoff, storedOrder);
                } finally {
                    deleteTestOrder(jdbcUrl, username, password, orderNumber);
                }
            }
            verifyBothOrderedOutcomes(executor, sessionFactory, jdbcUrl, username, password, testPrefix);
        } finally {
            executor.shutdownNow();
        }

        System.out.printf("MySQL %s simultaneous race rounds=%d, paymentWins=%d, timeoutWins=%d; "
                        + "both ordered outcomes passed%n", mysqlVersion, rounds, paymentWins, timeoutWins);
    }

    /**
     * 检查连接地址只能指向独立并发测试数据库
     *
     * @param jdbcUrl MySQL JDBC 连接地址
     */
    private void assertTestDatabaseUrl(String jdbcUrl) {
        URI databaseUri = URI.create(jdbcUrl.substring("jdbc:".length()));
        assertEquals("/" + TEST_DATABASE, databaseUri.getPath(),
                "真实并发测试只允许连接 sky_take_out_test");
    }

    /**
     * 加载项目实际订单 Mapper XML 并构造独立 MyBatis 会话工厂
     *
     * @param jdbcUrl MySQL JDBC 连接地址
     * @param username 测试数据库用户名
     * @param password 测试数据库密码
     * @return 加载真实 OrderMapper.xml 的会话工厂
     * @throws Exception Mapper XML 加载失败
     */
    private SqlSessionFactory createSqlSessionFactory(String jdbcUrl, String username, String password)
            throws Exception {
        UnpooledDataSource dataSource = new UnpooledDataSource("com.mysql.cj.jdbc.Driver", jdbcUrl, username, password);
        Environment environment = new Environment("mysql-order-race-test", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.getTypeAliasRegistry().registerAlias("Orders", Orders.class);
        configuration.addMapper(OrderMapper.class);
        try (InputStream mapperXml = getClass().getClassLoader().getResourceAsStream("mapper/OrderMapper.xml")) {
            if (mapperXml == null) {
                throw new IllegalStateException("找不到项目真实的 mapper/OrderMapper.xml");
            }
            new XMLMapperBuilder(mapperXml, configuration, "mapper/OrderMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    /**
     * 确认测试订单表使用支持行级锁的 InnoDB 引擎
     *
     * @param jdbcUrl MySQL JDBC 连接地址
     * @param username 测试数据库用户名
     * @param password 测试数据库密码
     * @throws Exception 数据库查询失败或存储引擎不符
     */
    private String verifyInnoDbTestTable(String jdbcUrl, String username, String password) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "select engine from information_schema.tables where table_schema = ? and table_name = 'orders'")) {
            assertEquals(TEST_DATABASE, connection.getCatalog(), "连接必须指向独立测试数据库");
            String mysqlVersion;
            try (Statement versionStatement = connection.createStatement();
                 ResultSet versionResult = versionStatement.executeQuery("select version()")) {
                versionResult.next();
                mysqlVersion = versionResult.getString(1);
            }
            statement.setString(1, TEST_DATABASE);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "测试库中必须先建立 orders 测试表");
                assertEquals("InnoDB", resultSet.getString(1), "并发测试要求 orders 表使用 InnoDB");
            }
            return mysqlVersion;
        }
    }

    /**
     * 验证订单时间恰好等于超时边界时条件更新不会取消订单
     *
     * @param sessionFactory 加载真实订单 Mapper 的会话工厂
     * @param jdbcUrl MySQL JDBC 连接地址
     * @param username 测试数据库用户名
     * @param password 测试数据库密码
     * @throws Exception 数据库边界测试失败
     */
    private void verifyTimeoutBoundary(SqlSessionFactory sessionFactory, String jdbcUrl, String username,
                                       String password) throws Exception {
        LocalDateTime boundary = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String orderNumber = "order-race-boundary-" + UUID.randomUUID();
        long orderId = insertOverdueUnpaidOrder(jdbcUrl, username, password, orderNumber, boundary);
        try (SqlSession session = sessionFactory.openSession(false)) {
            Orders targetOrder = Orders.builder().status(Orders.CANCELLED).payStatus(Orders.UN_PAID).build();
            int updatedRows = session.getMapper(OrderMapper.class).updateStatusIfCurrent(orderId,
                    Orders.PENDING_PAYMENT, Orders.UN_PAID, boundary, targetOrder);
            session.commit();
            assertEquals(0, updatedRows, "order_time 等于 cutoff 时不满足 SQL 的严格小于条件");
        } finally {
            deleteTestOrder(jdbcUrl, username, password, orderNumber);
        }
    }

    /**
     * 使用两条独立连接同时提交支付和超时取消条件更新
     *
     * @param executor 并发执行线程池
     * @param sessionFactory 加载真实订单 Mapper 的会话工厂
     * @param orderId 竞争更新的订单ID
     * @param timeoutCutoff 超时取消截止时间
     * @return 两个更新的影响行数及 MySQL 连接编号
     * @throws Exception 任一竞争线程失败
     */
    private RaceResult racePaymentAndTimeout(ExecutorService executor, SqlSessionFactory sessionFactory,
                                             long orderId, LocalDateTime timeoutCutoff) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<TransitionResult> paymentFuture = executor.submit(() -> runTransition(sessionFactory, orderId,
                ready, start, true, timeoutCutoff));
        Future<TransitionResult> timeoutFuture = executor.submit(() -> runTransition(sessionFactory, orderId,
                ready, start, false, timeoutCutoff));
        assertTrue(ready.await(10, TimeUnit.SECONDS), "竞争线程未能及时就绪");
        start.countDown();
        TransitionResult paymentResult = paymentFuture.get(30, TimeUnit.SECONDS);
        TransitionResult timeoutResult = timeoutFuture.get(30, TimeUnit.SECONDS);
        return new RaceResult(paymentResult.updatedRows, timeoutResult.updatedRows,
                paymentResult.connectionId, timeoutResult.connectionId);
    }

    /**
     * 稳定验证支付先完成和超时取消先完成时各自的最终状态
     *
     * @param executor 并发执行线程池
     * @param sessionFactory 加载真实订单 Mapper 的会话工厂
     * @param jdbcUrl MySQL JDBC 连接地址
     * @param username 测试数据库用户名
     * @param password 测试数据库密码
     * @param testPrefix 本次测试使用的订单编号前缀
     * @throws Exception 任一顺序验证失败
     */
    private void verifyBothOrderedOutcomes(ExecutorService executor, SqlSessionFactory sessionFactory,
                                           String jdbcUrl, String username, String password, String testPrefix)
            throws Exception {
        verifyOrderedOutcome(executor, sessionFactory, jdbcUrl, username, password,
                testPrefix + "payment-first", true);
        verifyOrderedOutcome(executor, sessionFactory, jdbcUrl, username, password,
                testPrefix + "timeout-first", false);
    }

    /**
     * 使用两个独立事务按指定顺序竞争同一订单并检查最终状态
     *
     * @param executor 并发执行线程池
     * @param sessionFactory 加载真实订单 Mapper 的会话工厂
     * @param jdbcUrl MySQL JDBC 连接地址
     * @param username 测试数据库用户名
     * @param password 测试数据库密码
     * @param orderNumber 本次顺序验证的订单编号
     * @param paymentFirst 是否让支付事务先提交
     * @throws Exception 数据库顺序竞争验证失败
     */
    private void verifyOrderedOutcome(ExecutorService executor, SqlSessionFactory sessionFactory,
                                      String jdbcUrl, String username, String password, String orderNumber,
                                      boolean paymentFirst) throws Exception {
        LocalDateTime timeoutCutoff = LocalDateTime.now().minusMinutes(15).truncatedTo(ChronoUnit.SECONDS);
        long orderId = insertOverdueUnpaidOrder(jdbcUrl, username, password, orderNumber,
                timeoutCutoff.minusSeconds(1));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch firstCommitted = new CountDownLatch(1);
        try {
            Future<TransitionResult> paymentFuture = executor.submit(() -> runOrderedTransition(sessionFactory,
                    orderId, ready, start, firstCommitted, paymentFirst, true, timeoutCutoff));
            Future<TransitionResult> timeoutFuture = executor.submit(() -> runOrderedTransition(sessionFactory,
                    orderId, ready, start, firstCommitted, !paymentFirst, false, timeoutCutoff));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "顺序验证线程未能及时就绪");
            start.countDown();
            TransitionResult paymentResult = paymentFuture.get(30, TimeUnit.SECONDS);
            TransitionResult timeoutResult = timeoutFuture.get(30, TimeUnit.SECONDS);

            assertEquals(1, paymentResult.updatedRows + timeoutResult.updatedRows);
            assertNotEquals(paymentResult.updatedRows, timeoutResult.updatedRows);
            assertNotEquals(paymentResult.connectionId, timeoutResult.connectionId);
            Orders storedOrder = selectOrder(sessionFactory, orderId);
            if (paymentFirst) {
                assertEquals(1, paymentResult.updatedRows);
                assertEquals(0, timeoutResult.updatedRows);
                assertEquals(Orders.TO_BE_CONFIRMED, storedOrder.getStatus());
                assertEquals(Orders.PAID, storedOrder.getPayStatus());
            } else {
                assertEquals(0, paymentResult.updatedRows);
                assertEquals(1, timeoutResult.updatedRows);
                assertEquals(Orders.CANCELLED, storedOrder.getStatus());
                assertEquals(Orders.UN_PAID, storedOrder.getPayStatus());
            }
        } finally {
            deleteTestOrder(jdbcUrl, username, password, orderNumber);
        }
    }

    /**
     * 在独立 MyBatis 事务中执行支付或超时取消状态更新
     *
     * @param sessionFactory 加载真实订单 Mapper 的会话工厂
     * @param orderId 竞争更新的订单ID
     * @param ready 两个线程均取得连接后的同步信号
     * @param start 同时开始执行更新的同步信号
     * @param paymentTransition 是否执行支付成功更新
     * @param timeoutCutoff 超时取消截止时间
     * @return 更新结果与 MySQL 连接编号
     * @throws Exception 连接失败、同步超时或 SQL 执行失败
     */
    private TransitionResult runTransition(SqlSessionFactory sessionFactory, long orderId, CountDownLatch ready,
                                           CountDownLatch start, boolean paymentTransition,
                                           LocalDateTime timeoutCutoff) throws Exception {
        try (SqlSession session = sessionFactory.openSession(false)) {
            long connectionId = readConnectionId(session.getConnection());
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("并发开始信号超时");
            }
            int updatedRows = updateOrderTransition(session, orderId, paymentTransition, timeoutCutoff);
            session.commit();
            return new TransitionResult(updatedRows, connectionId);
        }
    }

    /**
     * 在开始门闩同步后按指定顺序提交两个状态转换
     *
     * @param sessionFactory 加载真实订单 Mapper 的会话工厂
     * @param orderId 竞争更新的订单ID
     * @param ready 两个线程均取得连接后的同步信号
     * @param start 同时启动两个事务的同步信号
     * @param firstCommitted 先行事务提交完成的同步信号
     * @param isFirstTransition 当前是否为先行状态转换
     * @param paymentTransition 是否执行支付成功更新
     * @param timeoutCutoff 超时取消截止时间
     * @return 条件更新影响行数与 MySQL 连接编号
     * @throws Exception 数据库更新或同步等待失败
     */
    private TransitionResult runOrderedTransition(SqlSessionFactory sessionFactory, long orderId,
                                                  CountDownLatch ready, CountDownLatch start,
                                                  CountDownLatch firstCommitted, boolean isFirstTransition,
                                                  boolean paymentTransition, LocalDateTime timeoutCutoff)
            throws Exception {
        try (SqlSession session = sessionFactory.openSession(false)) {
            long connectionId = readConnectionId(session.getConnection());
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("顺序验证开始信号超时");
            }
            if (!isFirstTransition && !firstCommitted.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("先行状态转换未能及时提交");
            }
            int updatedRows = updateOrderTransition(session, orderId, paymentTransition, timeoutCutoff);
            session.commit();
            if (isFirstTransition) {
                firstCommitted.countDown();
            }
            return new TransitionResult(updatedRows, connectionId);
        }
    }

    /**
     * 通过实际订单 Mapper 执行支付或超时取消条件更新
     *
     * @param session 当前独立 MyBatis 事务
     * @param orderId 需要转换状态的订单ID
     * @param paymentTransition 是否执行支付成功更新
     * @param timeoutCutoff 超时取消截止时间
     * @return Mapper 返回的影响行数
     */
    private int updateOrderTransition(SqlSession session, long orderId, boolean paymentTransition,
                                      LocalDateTime timeoutCutoff) {
        Orders targetOrder = paymentTransition
                ? Orders.builder().status(Orders.TO_BE_CONFIRMED).payStatus(Orders.PAID)
                    .checkoutTime(LocalDateTime.now()).build()
                : Orders.builder().status(Orders.CANCELLED).payStatus(Orders.UN_PAID)
                    .cancelReason("超时自动取消").cancelTime(LocalDateTime.now()).build();
        return session.getMapper(OrderMapper.class).updateStatusIfCurrent(orderId,
                Orders.PENDING_PAYMENT, Orders.UN_PAID, paymentTransition ? null : timeoutCutoff, targetOrder);
    }

    /**
     * 查询当前连接在 MySQL 中的唯一连接编号
     *
     * @param connection 当前 MySQL 连接
     * @return MySQL 连接编号
     * @throws Exception 查询连接编号失败
     */
    private long readConnectionId(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select connection_id()")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    /**
     * 重复提交支付或超时取消条件后确认终态不会被覆盖
     *
     * @param sessionFactory 加载真实订单 Mapper 的会话工厂
     * @param orderId 已经完成状态转换的订单ID
     * @param timeoutCutoff 超时取消截止时间
     * @param storedOrder 当前数据库中的终态订单
     */
    private void assertRepeatedTransitionsDoNotOverwrite(SqlSessionFactory sessionFactory, long orderId,
                                                         LocalDateTime timeoutCutoff, Orders storedOrder) {
        try (SqlSession session = sessionFactory.openSession(false)) {
            OrderMapper mapper = session.getMapper(OrderMapper.class);
            Orders paymentTarget = Orders.builder().status(Orders.TO_BE_CONFIRMED).payStatus(Orders.PAID)
                    .checkoutTime(LocalDateTime.now()).build();
            Orders timeoutTarget = Orders.builder().status(Orders.CANCELLED).payStatus(Orders.UN_PAID)
                    .cancelReason("重复超时取消").cancelTime(LocalDateTime.now()).build();
            assertEquals(0, mapper.updateStatusIfCurrent(orderId, Orders.PENDING_PAYMENT, Orders.UN_PAID,
                    null, paymentTarget));
            assertEquals(0, mapper.updateStatusIfCurrent(orderId, Orders.PENDING_PAYMENT, Orders.UN_PAID,
                    timeoutCutoff, timeoutTarget));
            session.commit();
        }
        Orders orderAfterRepeatedUpdates = selectOrder(sessionFactory, orderId);
        assertEquals(storedOrder.getStatus(), orderAfterRepeatedUpdates.getStatus());
        assertEquals(storedOrder.getPayStatus(), orderAfterRepeatedUpdates.getPayStatus());
    }

    /**
     * 插入本轮专用的待付款未支付订单
     *
     * @param jdbcUrl MySQL JDBC 连接地址
     * @param username 测试数据库用户名
     * @param password 测试数据库密码
     * @param orderNumber 本轮唯一订单编号
     * @param orderTime 测试订单下单时间
     * @return 新插入的订单ID
     * @throws Exception 测试订单插入失败
     */
    private long insertOverdueUnpaidOrder(String jdbcUrl, String username, String password, String orderNumber,
                                          LocalDateTime orderTime) throws Exception {
        String sql = "insert into orders (number, status, order_time, pay_status) values (?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, orderNumber);
            statement.setInt(2, Orders.PENDING_PAYMENT);
            statement.setObject(3, orderTime);
            statement.setInt(4, Orders.UN_PAID);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                assertTrue(generatedKeys.next(), "测试订单未返回自增ID");
                return generatedKeys.getLong(1);
            }
        }
    }

    /**
     * 通过实际订单 Mapper 查询竞争后的订单状态
     *
     * @param sessionFactory 加载真实订单 Mapper 的会话工厂
     * @param orderId 需要查询的订单ID
     * @return 当前订单数据
     */
    private Orders selectOrder(SqlSessionFactory sessionFactory, long orderId) {
        try (SqlSession session = sessionFactory.openSession()) {
            return session.getMapper(OrderMapper.class).getById(orderId);
        }
    }

    /**
     * 删除当前集成测试生成的专用订单
     *
     * @param jdbcUrl MySQL JDBC 连接地址
     * @param username 测试数据库用户名
     * @param password 测试数据库密码
     * @param orderNumber 本轮唯一订单编号
     * @throws Exception 测试数据清理失败
     */
    private void deleteTestOrder(String jdbcUrl, String username, String password, String orderNumber)
            throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement("delete from orders where number = ?")) {
            statement.setString(1, orderNumber);
            statement.executeUpdate();
        }
    }

    private static class RaceResult {
        private final int paymentRows;
        private final int timeoutRows;
        private final long paymentConnectionId;
        private final long timeoutConnectionId;

        /**
         * 保存一轮订单状态竞争的更新结果
         *
         * @param paymentRows 支付更新的影响行数
         * @param timeoutRows 超时取消更新的影响行数
         * @param paymentConnectionId 支付线程的 MySQL 连接编号
         * @param timeoutConnectionId 超时线程的 MySQL 连接编号
         */
        private RaceResult(int paymentRows, int timeoutRows, long paymentConnectionId, long timeoutConnectionId) {
            this.paymentRows = paymentRows;
            this.timeoutRows = timeoutRows;
            this.paymentConnectionId = paymentConnectionId;
            this.timeoutConnectionId = timeoutConnectionId;
        }
    }

    private static class TransitionResult {
        private final int updatedRows;
        private final long connectionId;

        /**
         * 保存单次条件更新的影响行数和连接编号
         *
         * @param updatedRows Mapper 返回的影响行数
         * @param connectionId 执行更新的 MySQL 连接编号
         */
        private TransitionResult(int updatedRows, long connectionId) {
            this.updatedRows = updatedRows;
            this.connectionId = connectionId;
        }
    }
}
