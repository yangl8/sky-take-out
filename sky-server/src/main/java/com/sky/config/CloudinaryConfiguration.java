package com.sky.config;

import com.sky.properties.CloudinaryProperties;
import com.sky.utils.CloudinaryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CloudinaryConfiguration {

    @Bean
    public CloudinaryUtil cloudinaryUtil(CloudinaryProperties properties) {
        log.info("开始创建 Cloudinary 图片上传工具类对象");
        return new CloudinaryUtil(
                properties.getCloudName(),
                properties.getApiKey(),
                properties.getApiSecret());
    }
}
