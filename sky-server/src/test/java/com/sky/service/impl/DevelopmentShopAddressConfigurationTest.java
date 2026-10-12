package com.sky.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DevelopmentShopAddressConfigurationTest {

    @Test
    void loadsTheVerifiedDemoShopAddressFromDevelopmentProfile() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-dev.yml"));
        Properties properties = yaml.getObject();

        assertNotNull(properties);
        assertEquals("北京市朝阳区工人体育场北路8号", properties.getProperty("sky.shop.address"));
    }
}
