package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.DeliveryTimeValidationResult;
import com.sky.service.DeliveryTimeValidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryGeocodingTest {

    private static final long USER_ID = 7L;
    private static final long ADDRESS_ID = 10L;
    private static final String SHOP_COORDINATE = "116.400001,39.900001";
    private static final String USER_COORDINATE = "116.410001,39.910001";

    @Mock private AddressBookMapper addressBookMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderDetailMapper orderDetailMapper;
    @Mock private ShoppingCartMapper shoppingCartMapper;
    @Mock private DeliveryTimeValidationService deliveryTimeValidationService;

    @Spy
    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(USER_ID);
        ReflectionTestUtils.setField(orderService, "shopAddress", "北京市朝阳区新街大道1号");
        ReflectionTestUtils.setField(orderService, "amapKey", "test-key");
        lenient().when(deliveryTimeValidationService.validate(any(), any())).thenReturn(DeliveryTimeValidationResult.VALID);
    }

    @AfterEach
    void clearContext() {
        BaseContext.removeCurrentId();
    }

    @Test
    void sendsFullMunicipalAddressAndUsesVerifiedCoordinatesForDrivingRequest() {
        AddressBook addressBook = beijingAddress();
        doReturn(geocode("北京市朝阳区新街大道1号", "110105", SHOP_COORDINATE, "门牌号"))
                .doReturn(geocode("北京市朝阳区新街大道8号", "110105", USER_COORDINATE, "门牌号"))
                .doReturn(route(SHOP_COORDINATE, USER_COORDINATE, "5000"))
                .when(orderService).requestAmap(anyString(), anyMap());

        orderService.checkOutOfRange(addressBook);

        ArgumentCaptor<Map<String, String>> parameters = ArgumentCaptor.forClass(Map.class);
        verify(orderService, org.mockito.Mockito.times(3)).requestAmap(anyString(), parameters.capture());
        assertEquals("北京市朝阳区新街大道1号", parameters.getAllValues().get(0).get("address"));
        assertEquals("北京市朝阳区新街大道8号", parameters.getAllValues().get(1).get("address"));
        assertEquals(SHOP_COORDINATE, parameters.getAllValues().get(2).get("origin"));
        assertEquals(USER_COORDINATE, parameters.getAllValues().get(2).get("destination"));
        assertEquals("json", parameters.getAllValues().get(0).get("output"));
    }

    @Test
    void preservesInclusiveFiveKilometreRangeAtServiceBoundary() {
        AddressBook addressBook = beijingAddress();
        for (String distance : new String[] {"4999", "5000"}) {
            stubSuccessfulGeocodingAndRoute(distance);
            orderService.checkOutOfRange(addressBook);
        }
        stubSuccessfulGeocodingAndRoute("5001");
        assertThrows(OrderBusinessException.class, () -> orderService.checkOutOfRange(addressBook));
    }

    @Test
    void geocodingFailurePreventsOrderCreationAndCartClearing() {
        when(addressBookMapper.getByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(
                AddressBook.builder().id(ADDRESS_ID).userId(USER_ID).build());
        OrdersSubmitDTO request = new OrdersSubmitDTO();
        request.setAddressBookId(ADDRESS_ID);
        request.setDeliveryStatus(0);
        request.setEstimatedDeliveryTime(LocalDateTime.of(2026, 10, 12, 13, 0));

        assertThrows(OrderBusinessException.class, () -> orderService.submitOrder(request));

        verify(orderMapper, never()).insert(any());
        verify(orderDetailMapper, never()).insertBatch(any());
        verify(shoppingCartMapper, never()).deleteByUserId(USER_ID);
        verify(shoppingCartMapper, never()).list(any());
    }

    private AddressBook beijingAddress() {
        return AddressBook.builder()
                .id(ADDRESS_ID).userId(USER_ID)
                .provinceCode("11").cityCode("1101").districtCode("110105")
                .provinceName("北京市").cityName("市辖区").districtName("朝阳区")
                .detail("新街大道8号")
                .build();
    }

    private void stubSuccessfulGeocodingAndRoute(String distance) {
        doReturn(geocode("北京市朝阳区新街大道1号", "110105", SHOP_COORDINATE, "门牌号"))
                .doReturn(geocode("北京市朝阳区新街大道8号", "110105", USER_COORDINATE, "门牌号"))
                .doReturn(route(SHOP_COORDINATE, USER_COORDINATE, distance))
                .when(orderService).requestAmap(anyString(), anyMap());
    }

    private String geocode(String formattedAddress, String adcode, String location, String level) {
        return "{\"status\":\"1\",\"infocode\":\"10000\",\"geocodes\":[{\"formatted_address\":\""
                + formattedAddress + "\",\"adcode\":\"" + adcode + "\",\"location\":\"" + location
                + "\",\"level\":\"" + level + "\"}]}";
    }

    private String route(String origin, String destination, String distance) {
        return "{\"status\":\"1\",\"infocode\":\"10000\",\"route\":{\"origin\":\"" + origin
                + "\",\"destination\":\"" + destination + "\",\"paths\":[{\"distance\":\""
                + distance + "\"}]}}";
    }
}
