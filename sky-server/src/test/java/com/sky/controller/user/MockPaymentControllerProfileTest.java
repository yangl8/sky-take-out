package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.properties.JwtProperties;
import com.sky.service.OrderService;
import com.sky.utils.JwtUtil;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MockPaymentControllerProfileTest {

    private static final String MOCK_PAYMENT_PATH = "/user/order/mockPaySuccess/order-10/10";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(MockPaymentWebConfiguration.class);

    @Test
    void mockPaymentEndpointIsUnavailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            OrderService orderService = context.getBean(OrderService.class);
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

            mockMvc.perform(post(MOCK_PAYMENT_PATH).header("authentication", userToken(context)))
                    .andExpect(status().isNotFound());

            verify(orderService, never()).mockPaySuccessForUser("order-10", 10L);
        });
    }

    @Test
    void mockPaymentEndpointRemainsAvailableInExplicitDevelopmentProfile() throws Exception {
        contextRunner.withPropertyValues("spring.profiles.active=dev").run(context -> {
            OrderService orderService = context.getBean(OrderService.class);
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

            mockMvc.perform(post(MOCK_PAYMENT_PATH).header("authentication", userToken(context)))
                    .andExpect(status().isOk());

            verify(orderService).mockPaySuccessForUser("order-10", 10L);
        });
    }

    @Test
    void productionProfileBlocksMockPaymentEvenIfDevelopmentProfileAndFlagAreEnabled() {
        contextRunner.withPropertyValues(
                "spring.profiles.active=dev,prod",
                "sky.payment.mock-enabled=true").run(context -> {
            OrderService orderService = context.getBean(OrderService.class);
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

            mockMvc.perform(post(MOCK_PAYMENT_PATH).header("authentication", userToken(context)))
                    .andExpect(status().isNotFound());

            verify(orderService, never()).mockPaySuccessForUser("order-10", 10L);
        });
    }

    private String userToken(org.springframework.context.ApplicationContext context) {
        JwtProperties properties = context.getBean(JwtProperties.class);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, 100L);
        return JwtUtil.createJWT(properties.getUserSecretKey(), 60_000,
                claims);
    }

    @Configuration
    @EnableWebMvc
    @Import(MockPaymentController.class)
    static class MockPaymentWebConfiguration implements WebMvcConfigurer {

        @Bean
        JwtProperties jwtProperties() {
            JwtProperties properties = new JwtProperties();
            properties.setAdminSecretKey(randomBase64Key());
            properties.setUserSecretKey(randomBase64Key());
            properties.setAdminTokenName("token");
            properties.setUserTokenName("authentication");
            return properties;
        }

        @Bean
        OrderService orderService() {
            return mock(OrderService.class);
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(userInterceptor()).addPathPatterns("/user/**");
        }

        @Bean
        JwtTokenUserInterceptor userInterceptor() {
            return new JwtTokenUserInterceptor();
        }

        private static String randomBase64Key() {
            byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            return Base64.getEncoder().encodeToString(key);
        }
    }
}
