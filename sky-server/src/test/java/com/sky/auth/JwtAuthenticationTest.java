package com.sky.auth;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.interceptor.JwtTokenAdminInterceptor;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationTest {

    private JwtProperties properties;
    private JwtTokenAdminInterceptor adminInterceptor;
    private JwtTokenUserInterceptor userInterceptor;
    private HandlerMethod handler;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        properties = new JwtProperties();
        properties.setAdminSecretKey(randomBase64Key());
        properties.setUserSecretKey(randomBase64Key());
        properties.setAdminTokenName("token");
        properties.setUserTokenName("authentication");
        properties.afterPropertiesSet();

        adminInterceptor = new JwtTokenAdminInterceptor();
        userInterceptor = new JwtTokenUserInterceptor();
        ReflectionTestUtils.setField(adminInterceptor, "jwtProperties", properties);
        ReflectionTestUtils.setField(userInterceptor, "jwtProperties", properties);
        handler = new HandlerMethod(new TestHandler(), TestHandler.class.getMethod("handle"));
    }

    @AfterEach
    void clearContext() {
        BaseContext.removeCurrentId();
    }

    @Test
    void jwtShouldValidateSignatureAndDecodeBase64KeyBytes() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, 42L);
        String token = JwtUtil.createJWT(properties.getUserSecretKey(), 60_000, claims);

        Claims parsed = JwtUtil.parseJWT(properties.getUserSecretKey(), token);

        assertEquals(42L, ((Number) parsed.get(JwtClaimsConstant.USER_ID)).longValue());
        assertThrows(RuntimeException.class, () -> JwtUtil.parseJWT(properties.getAdminSecretKey(), token));
    }

    @Test
    void jwtKeysMustBeValidBase64AndAtLeastThirtyTwoBytes() {
        assertThrows(IllegalArgumentException.class, () -> JwtUtil.decodeSecretKey("short"));
        assertThrows(IllegalArgumentException.class, () -> JwtUtil.decodeSecretKey("%%%"));
        String validBase64ButTooShort = Base64.getEncoder().encodeToString(new byte[16]);
        assertThrows(IllegalArgumentException.class,
                () -> JwtUtil.decodeSecretKey(validBase64ButTooShort));
    }

    @Test
    void startupMustRejectMissingOrSharedSigningKeys() {
        JwtProperties missing = new JwtProperties();
        missing.setUserSecretKey(randomBase64Key());
        assertThrows(IllegalStateException.class, missing::afterPropertiesSet);

        JwtProperties shared = new JwtProperties();
        String oneKey = randomBase64Key();
        shared.setAdminSecretKey(oneKey);
        shared.setUserSecretKey(oneKey);
        assertThrows(IllegalStateException.class, shared::afterPropertiesSet);
    }

    @Test
    void userAndAdminTokensMustNotAuthenticateAcrossInterceptors() throws Exception {
        String userToken = token(JwtClaimsConstant.USER_ID, 100L, properties.getUserSecretKey());
        String adminToken = token(JwtClaimsConstant.EMP_ID, 200L, properties.getAdminSecretKey());

        MockHttpServletRequest adminRequest = new MockHttpServletRequest();
        adminRequest.addHeader(properties.getAdminTokenName(), userToken);
        MockHttpServletResponse adminResponse = new MockHttpServletResponse();
        assertFalse(adminInterceptor.preHandle(adminRequest, adminResponse, handler));
        assertEquals(401, adminResponse.getStatus());
        assertNull(BaseContext.getCurrentId());

        MockHttpServletRequest userRequest = new MockHttpServletRequest();
        userRequest.addHeader(properties.getUserTokenName(), adminToken);
        MockHttpServletResponse userResponse = new MockHttpServletResponse();
        assertFalse(userInterceptor.preHandle(userRequest, userResponse, handler));
        assertEquals(401, userResponse.getStatus());
        assertNull(BaseContext.getCurrentId());
    }

    @Test
    void userInterceptorShouldRejectTamperedSignatureAndLeaveNoIdentity() throws Exception {
        String validToken = token(JwtClaimsConstant.USER_ID, 100L, properties.getUserSecretKey());
        String[] tokenParts = validToken.split("\\.");
        assertEquals(3, tokenParts.length);
        char originalSignatureCharacter = tokenParts[2].charAt(0);
        tokenParts[2] = (originalSignatureCharacter == 'A' ? "B" : "A") + tokenParts[2].substring(1);
        String tamperedToken = String.join(".", tokenParts);

        // The header and payload remain valid, including the required user id claim.
        assertEquals(100L, ((Number) JwtUtil.parseJWT(properties.getUserSecretKey(), validToken)
                .get(JwtClaimsConstant.USER_ID)).longValue());
        assertThrows(RuntimeException.class,
                () -> JwtUtil.parseJWT(properties.getUserSecretKey(), tamperedToken));

        BaseContext.setCurrentId(999L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(properties.getUserTokenName(), tamperedToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(userInterceptor.preHandle(request, response, handler));

        assertEquals(401, response.getStatus());
        assertNull(BaseContext.getCurrentId());
    }

    @Test
    void bothInterceptorsClearIdentityWhenRequestCompletes() throws Exception {
        MockHttpServletRequest userRequest = new MockHttpServletRequest();
        userRequest.addHeader(properties.getUserTokenName(),
                token(JwtClaimsConstant.USER_ID, 100L, properties.getUserSecretKey()));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(userInterceptor.preHandle(userRequest, response, handler));
        assertEquals(100L, BaseContext.getCurrentId());
        userInterceptor.afterCompletion(userRequest, response, handler, null);
        assertNull(BaseContext.getCurrentId());

        MockHttpServletRequest adminRequest = new MockHttpServletRequest();
        adminRequest.addHeader(properties.getAdminTokenName(),
                token(JwtClaimsConstant.EMP_ID, 200L, properties.getAdminSecretKey()));

        assertTrue(adminInterceptor.preHandle(adminRequest, response, handler));
        assertEquals(200L, BaseContext.getCurrentId());
        adminInterceptor.afterCompletion(adminRequest, response, handler, null);
        assertNull(BaseContext.getCurrentId());
    }

    @Test
    void rejectedRequestClearsAnyStaleThreadContext() throws Exception {
        BaseContext.setCurrentId(999L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(userInterceptor.preHandle(request, response, handler));

        assertNull(BaseContext.getCurrentId());
    }

    private String token(String claimName, Long id, String key) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(claimName, id);
        return JwtUtil.createJWT(key, 60_000, claims);
    }

    private String randomBase64Key() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    public static class TestHandler {
        public void handle() { }
    }
}
