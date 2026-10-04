package com.sky.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.util.Map;

public class CloudinaryUtil {

    private final Cloudinary cloudinary;

    public CloudinaryUtil(String cloudName, String apiKey, String apiSecret) {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    public String upload(byte[] bytes) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                "resource_type", "image",
                "folder", "sky-take-out"));
        return result.get("secure_url").toString();
    }
}
