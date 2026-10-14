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
 * Verifies the administrator pending-delivery ETA ordering branch
 */
class OrderMapperEtaOrderingTest {

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
