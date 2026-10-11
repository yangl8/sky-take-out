package com.sky.controller.notify;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.properties.WeChatProperties;
import com.sky.service.OrderService;
import com.wechat.pay.contrib.apache.httpclient.auth.CertificatesVerifier;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.notification.Notification;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationHandler;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationRequest;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.time.OffsetDateTime;

/**
 * 支付回调相关接口
 */
@RestController
@RequestMapping("/notify")
@Slf4j
public class PayNotifyController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private WeChatProperties weChatProperties;

    /**
     * 支付成功回调
     *
     * @param request
     */
    @RequestMapping("/paySuccess")
    public void paySuccessNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //读取数据
        String body = readData(request);
        log.info("支付成功回调：{}", body);

        //数据解密
        String plainText = verifyAndDecryptData(request, body);
        log.info("解密后的文本：{}", plainText);

        JSONObject jsonObject = JSON.parseObject(plainText);
        if (!"SUCCESS".equals(jsonObject.getString("trade_state"))) {
            throw new IllegalArgumentException("微信支付回调交易状态不是成功");
        }
        String outTradeNo = jsonObject.getString("out_trade_no");//商户平台订单号
        String transactionId = jsonObject.getString("transaction_id");//微信支付交易号
        String successTimeValue = jsonObject.getString("success_time");//微信支付成功时间

        log.info("商户平台订单号：{}", outTradeNo);
        log.info("微信支付交易号：{}", transactionId);

        //业务处理，修改订单状态、来单提醒
        if (successTimeValue == null || successTimeValue.trim().isEmpty()) {
            throw new IllegalArgumentException("微信支付回调缺少支付成功时间");
        }
        orderService.paySuccess(outTradeNo, OffsetDateTime.parse(successTimeValue));

        //给微信响应
        responseToWeixin(response);
    }

    /**
     * 读取数据
     *
     * @param request
     * @return
     * @throws Exception
     */
    private String readData(HttpServletRequest request) throws Exception {
        BufferedReader reader = request.getReader();
        StringBuilder result = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append(line);
        }
        return result.toString();
    }

    /**
     * 验签并解密微信支付通知内容
     *
     * @param request 携带微信支付签名头的回调请求
     * @param body 微信支付通知原始请求体
     * @return 验签通过并解密后的通知内容
     * @throws Exception 证书读取、验签或通知解密失败
     */
    private String verifyAndDecryptData(HttpServletRequest request, String body) throws Exception {
        X509Certificate platformCertificate;
        try (FileInputStream certificateStream = new FileInputStream(
                weChatProperties.getWeChatPayCertFilePath())) {
            platformCertificate = PemUtil.loadCertificate(certificateStream);
        }
        Verifier verifier = new CertificatesVerifier(Collections.singletonList(platformCertificate));
        NotificationRequest notificationRequest = new NotificationRequest.Builder()
                .withSerialNumber(request.getHeader("Wechatpay-Serial"))
                .withTimestamp(request.getHeader("Wechatpay-Timestamp"))
                .withNonce(request.getHeader("Wechatpay-Nonce"))
                .withSignature(request.getHeader("Wechatpay-Signature"))
                .withBody(body)
                .build();
        Notification notification = new NotificationHandler(verifier,
                weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8)).parse(notificationRequest);
        return notification.getDecryptData();
    }

    /**
     * 给微信响应
     * @param response
     */
    private void responseToWeixin(HttpServletResponse response) throws Exception{
        response.setStatus(200);
        HashMap<Object, Object> map = new HashMap<>();
        map.put("code", "SUCCESS");
        map.put("message", "SUCCESS");
        response.setHeader("Content-type", ContentType.APPLICATION_JSON.toString());
        response.getOutputStream().write(JSONUtils.toJSONString(map).getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }
}
