package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.cloudinary")
@Data
public class CloudinaryProperties {

    private String cloudName;
    private String apiKey;
    private String apiSecret;
}
