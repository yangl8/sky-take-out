package com.sky.mapper;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证管理端待配送订单 ETA 排序分支
 */
class OrderMapperEtaOrderingTest {

    /**
     * 验证待配送查询先按送达时间排序且筛选条件仍应用于同一查询
     *
     * @throws Exception Mapper XML 解析失败
     */
    @Test
    void deliveryStatusShouldSortByEtaWithNullsLastAndStableTies() throws Exception {
        OrdersPageQueryDTO query = new OrdersPageQueryDTO();
        query.setStatus(3);
        query.setNumber("ETA-");
        query.setPhone("123");

        String sql = pageQuerySql(query);
        String ordering = sql.substring(sql.indexOf("order by"));

        assertTrue(sql.contains("number like concat"));
        assertTrue(sql.contains("phone like concat"));
        assertTrue(ordering.startsWith("order by (estimated_delivery_time is null) asc, estimated_delivery_time asc"));
        assertTrue(ordering.contains("order_time asc, id asc"));
        assertFalse(ordering.contains("delivery_status"), "立即单与预约单应共同按 ETA 排序");
    }

    /**
     * 验证非待配送状态和未指定状态保留下单时间倒序
     *
     * @throws Exception Mapper XML 解析失败
     */
    @Test
    void otherStatusesShouldKeepOrderTimeDescending() throws Exception {
        for (Integer status : new Integer[]{null, 1, 2, 4, 5, 6}) {
            OrdersPageQueryDTO query = new OrdersPageQueryDTO();
            query.setStatus(status);

            String sql = pageQuerySql(query);
            String ordering = sql.substring(sql.indexOf("order by"));

            assertTrue(ordering.startsWith("order by order_time desc"), "status=" + status);
            assertFalse(ordering.contains("estimated_delivery_time"), "status=" + status);
        }
    }

    /**
     * 解析订单分页查询并返回绑定后的 SQL
     *
     * @param query 订单筛选条件
     * @return MyBatis 生成的分页查询 SQL
     * @throws Exception Mapper XML 解析失败
     */
    private String pageQuerySql(OrdersPageQueryDTO query) throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("Orders", Orders.class);
        configuration.getTypeAliasRegistry().registerAlias("OrderDetail", OrderDetail.class);
        configuration.addMapper(OrderMapper.class);
        try (InputStream mapperXml = getClass().getClassLoader().getResourceAsStream("mapper/OrderMapper.xml")) {
            XMLMapperBuilder parser = new XMLMapperBuilder(mapperXml, configuration,
                    "mapper/OrderMapper.xml", configuration.getSqlFragments());
            parser.parse();
        }

        MappedStatement statement = configuration.getMappedStatement("com.sky.mapper.OrderMapper.pageQuery");
        BoundSql boundSql = statement.getBoundSql(query);
        return boundSql.getSql().replaceAll("\\s+", " ").toLowerCase();
    }
}
