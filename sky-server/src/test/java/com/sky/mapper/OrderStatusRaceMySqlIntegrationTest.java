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

class OrderStatusRaceMySqlIntegrationTest {

    private static final String JDBC_URL_ENV = "SKY_ORDER_RACE_DB_URL";
    private static final String JDBC_USER_ENV = "SKY_ORDER_RACE_DB_USER";
    private static final String JDBC_PASSWORD_ENV = "SKY_ORDER_RACE_DB_PASSWORD";
    private static final String ROUNDS_ENV = "SKY_ORDER_RACE_ROUNDS";
    private static final String TEST_DATABASE = "sky_take_out_test";

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

    private void assertTestDatabaseUrl(String jdbcUrl) {
        URI databaseUri = URI.create(jdbcUrl.substring("jdbc:".length()));
        assertEquals("/" + TEST_DATABASE, databaseUri.getPath(),
                "真实并发测试只允许连接 sky_take_out_test");
    }

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

    private void verifyBothOrderedOutcomes(ExecutorService executor, SqlSessionFactory sessionFactory,
                                           String jdbcUrl, String username, String password, String testPrefix)
            throws Exception {
        verifyOrderedOutcome(executor, sessionFactory, jdbcUrl, username, password,
                testPrefix + "payment-first", true);
        verifyOrderedOutcome(executor, sessionFactory, jdbcUrl, username, password,
                testPrefix + "timeout-first", false);
    }

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

    private long readConnectionId(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select connection_id()")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

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

    private Orders selectOrder(SqlSessionFactory sessionFactory, long orderId) {
        try (SqlSession session = sessionFactory.openSession()) {
            return session.getMapper(OrderMapper.class).getById(orderId);
        }
    }

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

        private TransitionResult(int updatedRows, long connectionId) {
            this.updatedRows = updatedRows;
            this.connectionId = connectionId;
        }
    }
}
