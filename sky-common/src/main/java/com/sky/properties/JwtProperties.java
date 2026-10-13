package com.sky.properties;

import lombok.Data;
import lombok.ToString;
import com.sky.utils.JwtUtil;
import java.security.MessageDigest;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.jwt")
@Data
public class JwtProperties implements InitializingBean {

    @ToString.Exclude
    private String adminSecretKey;
    private long adminTtl;
    private String adminTokenName;

    @ToString.Exclude
    private String userSecretKey;
    private long userTtl;
    private String userTokenName;

    @Override
    public void afterPropertiesSet() {
        byte[] adminKey = decodeConfiguredKey(adminSecretKey, "JWT_ADMIN_SECRET_KEY");
        byte[] userKey = decodeConfiguredKey(userSecretKey, "JWT_USER_SECRET_KEY");
        if (MessageDigest.isEqual(adminKey, userKey)) {
            throw new IllegalStateException("JWT_ADMIN_SECRET_KEY and JWT_USER_SECRET_KEY must be different");
        }
    }

    private byte[] decodeConfiguredKey(String key, String environmentVariable) {
        try {
            return JwtUtil.decodeSecretKey(key);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(environmentVariable
                    + " must be set to a Base64-encoded random key of at least 32 bytes", exception);
        }
    }

}
