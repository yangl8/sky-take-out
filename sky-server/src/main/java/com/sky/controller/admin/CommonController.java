package com.sky.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.CloudinaryUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/admin/common")
@Api(tags = "General endpoints")
@Slf4j
public class CommonController {
    
    @Autowired
    private CloudinaryUtil cloudinaryUtil;

    @PostMapping("/upload")
    @ApiOperation("Upload file")
    public Result<String> upload(MultipartFile file){
        log.info("Uploading file: {}", file);
        
        try {
            String filePath = cloudinaryUtil.upload(file.getBytes());
            return Result.success(filePath);
        } catch (Exception e) {
            log.error("File upload failed", e);
        }
        
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
