package com.sky.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.json.JacksonObjectMapper;
import com.sky.result.Result;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderSubmitRequestBindingTest {

    private final ObjectMapper objectMapper = new JacksonObjectMapper();

    @Test
    void bindsImmediateDeliveryRequestProducedByMiniProgram() throws Exception {
        String estimateResponse = objectMapper.writeValueAsString(Result.success(LocalDateTime.of(2026, 10, 11, 17, 48)));
        JsonNode estimateData = objectMapper.readTree(estimateResponse).get("data");
        assertEquals("2026-10-11 17:48", estimateData.asText());

        OrdersSubmitDTO request = objectMapper.readValue(miniProgramRequest("2026-10-11 17:48:00", 1), OrdersSubmitDTO.class);

        assertEquals(Integer.valueOf(1), request.getDeliveryStatus());
        assertEquals(LocalDateTime.of(2026, 10, 11, 17, 48), request.getEstimatedDeliveryTime());
    }

    @Test
    void rejectsMinutePrecisionFromEstimatedTimeEndpointWithoutSeconds() {
        assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(
                miniProgramRequest("2026-10-11 17:48", 1), OrdersSubmitDTO.class));
    }

    @Test
    void bindsScheduledDeliveryRequestProducedByMiniProgram() throws Exception {
        OrdersSubmitDTO request = objectMapper.readValue(miniProgramRequest("2026-10-12 10:30:00", 0), OrdersSubmitDTO.class);

        assertEquals(Integer.valueOf(0), request.getDeliveryStatus());
        assertEquals(LocalDateTime.of(2026, 10, 12, 10, 30), request.getEstimatedDeliveryTime());
    }

    private String miniProgramRequest(String estimatedDeliveryTime, int deliveryStatus) {
        return "{" +
                "\"payMethod\":1," +
                "\"addressBookId\":10," +
                "\"remark\":\"\", " +
                "\"estimatedDeliveryTime\":\"" + estimatedDeliveryTime + "\"," +
                "\"deliveryStatus\":" + deliveryStatus + "," +
                "\"tablewareStatus\":0," +
                "\"tablewareNumber\":0," +
                "\"packAmount\":2," +
                "\"amount\":183.00," +
                "\"shopId\":1," +
                "\"deliveryFee\":0" +
                "}";
    }
}
