package com.sky.service.impl;

import com.sky.entity.AddressBook;
import com.sky.exception.OrderBusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeliveryLocationValidatorTest {

    @Test
    void buildsAddressForFourMunicipalitiesWithoutPlaceholderCity() {
        assertEquals("北京市朝阳区新街大道1号", DeliveryLocationValidator.buildGeocodingAddress(address(
                "11", "1101", "110105", "北京市", "市辖区", "朝阳区", "新街大道1号")));
        assertEquals("上海市浦东新区世纪大道1号", DeliveryLocationValidator.buildGeocodingAddress(address(
                "31", "3101", "310115", "上海市", "市辖区", "浦东新区", "世纪大道1号")));
        assertEquals("天津市和平区南京路1号", DeliveryLocationValidator.buildGeocodingAddress(address(
                "12", "1201", "120101", "天津市", "市辖区", "和平区", "南京路1号")));
        assertEquals("重庆市渝中区解放碑1号", DeliveryLocationValidator.buildGeocodingAddress(address(
                "50", "5001", "500103", "重庆市", "市辖区", "渝中区", "解放碑1号")));
    }

    @Test
    void buildsOrdinaryProvinceAddressWithAllThreeAdministrativeLevels() {
        assertEquals("山东省济南市历下区泉城路1号", DeliveryLocationValidator.buildGeocodingAddress(address(
                "37", "3701", "370102", "山东省", "济南市", "历下区", "泉城路1号")));
    }

    @Test
    void keepsUserDetailTextUntranslatedAndUnmodified() {
        assertEquals("北京市朝阳区Rue Xinjie", DeliveryLocationValidator.buildGeocodingAddress(address(
                "11", "1101", "110105", "北京市", "市辖区", "朝阳区", "Rue Xinjie")));
    }

    @Test
    void rejectsMissingOrInconsistentAddressFields() {
        assertInvalid(address("", "1101", "110105", "北京市", "市辖区", "朝阳区", "新街大道1号"));
        assertInvalid(address("37", "", "370102", "山东省", "", "历下区", "泉城路1号"));
        assertInvalid(address("37", "3701", "", "山东省", "济南市", "历下区", "泉城路1号"));
        assertInvalid(address("37", "3701", "370102", "山东省", "济南市", "历下区", "  "));
        assertInvalid(address("11", "1101", "110105", "上海市", "市辖区", "朝阳区", "新街大道1号"));
        assertInvalid(address("37", "3701", "370102", "山东省", "市辖区", "历下区", "泉城路1号"));
    }

    @Test
    void acceptsOnlyOnePreciseCandidateMatchingDistrictAndDetail() {
        String body = geocode(candidate("北京市朝阳区新街大道1号", "110105", "116.400001,39.900001", "门牌号"));
        assertEquals("116.400001,39.900001", DeliveryLocationValidator.resolveGeocodeCoordinate(
                body, "新街大道1号", "110105", "收货地址"));
    }

    @Test
    void acceptsObservedAmapCandidatesForDemoShopAndCustomerAndKeepsRouteInRange() {
        String exactShopCandidate = "{\"formatted_address\":\"北京市朝阳区工人体育场北路8号\",\"province\":\"北京市\","
                + "\"city\":\"北京市\",\"district\":\"朝阳区\",\"adcode\":\"110105\","
                + "\"location\":\"116.453574,39.932195\",\"level\":\"门址\","
                + "\"street\":\"工人体育场北路\",\"number\":\"8号\"}";
        assertEquals("116.453574,39.932195", DeliveryLocationValidator.resolveGeocodeCoordinate(
                geocode(exactShopCandidate), "北京市朝阳区工人体育场北路8号", null, "店铺地址"));
        String shopResponse = "{\"status\":\"1\",\"info\":\"OK\",\"count\":\"2\",\"geocodes\":["
                + exactShopCandidate + ","
                + "{\"formatted_address\":\"北京市朝阳区工人体育场北路6号\",\"province\":\"北京市\","
                + "\"city\":\"北京市\",\"district\":\"朝阳区\",\"adcode\":\"110105\","
                + "\"location\":\"116.457167,39.933032\",\"level\":\"门牌号\","
                + "\"street\":\"工人体育场北路\",\"number\":[]}] }";
        String customerResponse = "{\"status\":\"1\",\"info\":\"OK\",\"count\":\"2\",\"geocodes\":["
                + "{\"formatted_address\":\"北京市朝阳区三里屯路19号\",\"province\":\"北京市\","
                + "\"city\":\"北京市\",\"district\":\"朝阳区\",\"adcode\":\"110105\","
                + "\"location\":\"116.454281,39.935266\",\"level\":\"门址\","
                + "\"street\":\"三里屯路\",\"number\":\"19号\"},"
                + "{\"formatted_address\":\"北京市朝阳区三里屯路8号\",\"province\":\"北京市\","
                + "\"city\":\"北京市\",\"district\":\"朝阳区\",\"adcode\":\"110105\","
                + "\"location\":\"116.456022,39.942445\",\"level\":\"门牌号\","
                + "\"street\":\"三里屯路\",\"number\":[]}] }";

        String shopCoordinate = DeliveryLocationValidator.resolveGeocodeCoordinate(
                shopResponse, "北京市朝阳区工人体育场北路8号", null, "店铺地址");
        String customerCoordinate = DeliveryLocationValidator.resolveGeocodeCoordinate(
                customerResponse, "三里屯路19号", "110105", "收货地址");
        String routeResponse = "{\"status\":\"1\",\"info\":\"OK\",\"infocode\":\"10000\","
                + "\"count\":\"3\",\"route\":{\"origin\":\"116.453574,39.932195\","
                + "\"destination\":\"116.454281,39.935266\",\"paths\":["
                + "{\"distance\":\"1995\"},{\"distance\":\"2870\"},{\"distance\":\"3501\"}]}}";
        BigDecimal distance = DeliveryLocationValidator.resolveDrivingDistance(
                routeResponse, shopCoordinate, customerCoordinate);

        assertEquals("116.453574,39.932195", shopCoordinate);
        assertEquals("116.454281,39.935266", customerCoordinate);
        assertEquals(new BigDecimal("1995"), distance);
        DeliveryLocationValidator.requireWithinDeliveryRange(distance);
    }

    @Test
    void acceptsDocumentedGeocodeResponseWithoutInfocodeAndNormalizedPunctuation() {
        String candidate = "{\"formatted_address\":\"北京市朝阳区新街大道 8 号\",\"adcode\":\"110105\","
                + "\"location\":\"116.400001,39.900001\",\"level\":\"门牌号\"}";
        String response = "{\"status\":\"1\",\"count\":\"1\",\"info\":\"OK\",\"geocodes\":["
                + candidate + "]}";
        assertEquals("116.400001,39.900001", DeliveryLocationValidator.resolveGeocodeCoordinate(
                response, "新街大道，8号", "110105", "收货地址"));
    }

    @Test
    void acceptsPrecisePoiAndOnlyOmittedIndoorSuffixWhenBaseAddressMatches() {
        assertEquals("116.400001,39.900001", DeliveryLocationValidator.resolveGeocodeCoordinate(
                geocode(candidate("北京市朝阳区望京西园四区5号楼", "110105", "116.400001,39.900001", "单元号")),
                "望京西园四区5号楼2单元501室", "110105", "收货地址"));
        assertEquals("116.400001,39.900001", DeliveryLocationValidator.resolveGeocodeCoordinate(
                geocode(candidate("北京市朝阳区奥林匹克公园南门", "110105", "116.400001,39.900001", "兴趣点")),
                "奥林匹克公园南门", "110105", "收货地址"));
        assertEquals("109.4,24.3", DeliveryLocationValidator.resolveGeocodeCoordinate(
                geocode(candidate("广西壮族自治区柳州市鱼峰区德润路华润凯旋门", "450203", "109.4,24.3", "住宅区")),
                "德润路华润凯旋门", "450203", "收货地址"));
    }

    @Test
    void supportsDocumentedStreetAndNumberWhenFormattedAddressIsAbsent() {
        String candidate = "{\"province\":\"北京市\",\"city\":\"北京市\",\"district\":\"朝阳区\","
                + "\"street\":\"新街大道\",\"number\":\"8号\",\"adcode\":\"110105\","
                + "\"location\":\"116.400001,39.900001\",\"level\":\"门牌号\"}";
        assertEquals("116.400001,39.900001", DeliveryLocationValidator.resolveGeocodeCoordinate(
                geocode(candidate), "新街大道8号", "110105", "收货地址"));
        assertGeocodeRejected(geocode("{\"adcode\":\"110105\",\"location\":\"116.4,39.9\","
                + "\"level\":\"门牌号\"}"), "新街大道8号", "110105");
    }

    @Test
    void deduplicatesEquivalentCandidatesButRejectsDistinctPreciseLocations() {
        String first = candidate("北京市朝阳区新街大道8号", "110105", "116.400001,39.900001", "门牌号");
        String sameRoundedLocation = candidate("北京市朝阳区新街大道8号", "110105", "116.4000014,39.9000014", "门牌号");
        assertEquals("116.400001,39.900001", DeliveryLocationValidator.resolveGeocodeCoordinate(
                geocode(first, sameRoundedLocation), "新街大道8号", "110105", "收货地址"));
        assertGeocodeRejected(geocode(first,
                candidate("北京市朝阳区新街大道8号", "110105", "116.401,39.900001", "门牌号")),
                "新街大道8号", "110105");
    }

    @Test
    void rejectsPoiWithoutExactPlaceNameAndInconsistentResultCount() {
        assertGeocodeRejected(geocode(candidate("北京市朝阳区奥林匹克公园", "110105", "116.4,39.9", "兴趣点")),
                "望京西园", "110105");
        String mismatch = "{\"status\":\"1\",\"count\":\"2\",\"geocodes\":["
                + candidate("北京市朝阳区新街大道8号", "110105", "116.4,39.9", "门牌号") + "]}";
        assertGeocodeRejected(mismatch, "新街大道8号", "110105");
    }

    @Test
    void rejectsFailedEmptyMalformedOrCoarseGeocodeResponses() {
        assertGeocodeRejected("", "新街大道1号", "110105");
        assertGeocodeRejected("{\"status\":\"0\",\"infocode\":\"10001\"}", "新街大道1号", "110105");
        assertGeocodeRejected("{\"status\":\"1\",\"infocode\":\"10000\",\"geocodes\":[]}", "新街大道1号", "110105");
        assertGeocodeRejected(geocode(candidate("北京市朝阳区新街大道", "110105", "116.4,39.9", "道路")), "新街大道", "110105");
    }

    @Test
    void rejectsInvalidCoordinatesAndWrongAdministrativeCode() {
        assertGeocodeRejected(geocode(candidate("北京市朝阳区新街大道1号", "110105", "181,39", "门牌号")), "新街大道1号", "110105");
        assertGeocodeRejected(geocode(candidate("北京市朝阳区新街大道1号", "110105", "116.4,NaN", "门牌号")), "新街大道1号", "110105");
        assertGeocodeRejected(geocode(candidate("北京市朝阳区新街大道1号", "110108", "116.4,39.9", "门牌号")), "新街大道1号", "110105");
    }

    @Test
    void filtersCoarseCandidatesButRejectsMultiplePreciseMatches() {
        String precise = candidate("北京市朝阳区新街大道1号", "110105", "116.4,39.9", "门牌号");
        String coarse = candidate("北京市朝阳区新街大道", "110105", "116.4,39.8", "道路");
        assertEquals("116.4,39.9", DeliveryLocationValidator.resolveGeocodeCoordinate(
                geocode(coarse, precise), "新街大道1号", "110105", "收货地址"));
        assertGeocodeRejected(geocode(precise, candidate(
                "北京市朝阳区新街大道1号", "110105", "116.5,39.9", "门牌号")), "新街大道1号", "110105");
    }

    @Test
    void allowsMissingOptionalCountButRejectsWrongInfocodeWhenPresent() {
        String body = "{\"status\":\"1\",\"infocode\":\"10001\",\"geocodes\":["
                + candidate("北京市朝阳区新街大道1号", "110105", "116.4,39.9", "门牌号") + "]}";
        assertGeocodeRejected(body, "新街大道1号", "110105");
    }

    @Test
    void validatesRouteStatusEndpointsPathsAndDistance() {
        assertRouteRejected("{\"status\":\"0\",\"infocode\":\"10001\"}");
        assertRouteRejected(route("116.4,39.9", "116.5,39.9", "[]"));
        assertRouteRejected(route("116.4,39.9", "116.6,39.9", "[{\"distance\":\"10\"}]"));
        assertRouteRejected(route("116.4,39.9", "116.5,39.9", "[]"));
        assertRouteRejected(route("116.4,39.9", "116.5,39.9", "[{}]"));
        assertRouteRejected(route("116.4,39.9", "116.5,39.9", "[{\"distance\":\"NaN\"}]"));
        assertRouteRejected(route("116.4,39.9", "116.5,39.9", "[{\"distance\":\"-1\"}]"));
        assertRouteRejected("{\"status\":\"1\",\"infocode\":\"10000\",\"count\":\"2\","
                + "\"route\":{\"origin\":\"116.4,39.9\",\"destination\":\"116.5,39.9\","
                + "\"paths\":[{\"distance\":\"10\"}]}} ");
        assertEquals(new BigDecimal("4999"), DeliveryLocationValidator.resolveDrivingDistance(
                route("116.4,39.9", "116.5,39.9", "[{\"distance\":\"4999\"}]"), "116.4,39.9", "116.5,39.9"));
        assertEquals(new BigDecimal("10"), DeliveryLocationValidator.resolveDrivingDistance(
                "{\"status\":\"1\",\"route\":{\"origin\":\"116.4,39.9\","
                        + "\"destination\":\"116.5,39.9\",\"paths\":[{\"distance\":\"10\"}]}}",
                "116.4,39.9", "116.5,39.9"));
    }

    @Test
    void keepsTheFiveKilometreBoundaryInclusive() {
        DeliveryLocationValidator.requireWithinDeliveryRange(new BigDecimal("4999"));
        DeliveryLocationValidator.requireWithinDeliveryRange(new BigDecimal("5000"));
        assertThrows(OrderBusinessException.class,
                () -> DeliveryLocationValidator.requireWithinDeliveryRange(new BigDecimal("5001")));
    }

    private AddressBook address(String provinceCode, String cityCode, String districtCode,
                                String provinceName, String cityName, String districtName, String detail) {
        return AddressBook.builder()
                .provinceCode(provinceCode).cityCode(cityCode).districtCode(districtCode)
                .provinceName(provinceName).cityName(cityName).districtName(districtName).detail(detail)
                .build();
    }

    private void assertInvalid(AddressBook addressBook) {
        assertThrows(OrderBusinessException.class, () -> DeliveryLocationValidator.buildGeocodingAddress(addressBook));
    }

    private void assertGeocodeRejected(String body, String detail, String districtCode) {
        assertThrows(OrderBusinessException.class,
                () -> DeliveryLocationValidator.resolveGeocodeCoordinate(body, detail, districtCode, "收货地址"));
    }

    private void assertRouteRejected(String body) {
        assertThrows(OrderBusinessException.class,
                () -> DeliveryLocationValidator.resolveDrivingDistance(body, "116.4,39.9", "116.5,39.9"));
    }

    private String candidate(String formattedAddress, String adcode, String location, String level) {
        return "{\"formatted_address\":\"" + formattedAddress + "\",\"adcode\":\"" + adcode
                + "\",\"location\":\"" + location + "\",\"level\":\"" + level + "\"}";
    }

    private String geocode(String... candidates) {
        return "{\"status\":\"1\",\"infocode\":\"10000\",\"geocodes\":[" + String.join(",", candidates) + "]}";
    }

    private String route(String origin, String destination, String paths) {
        return "{\"status\":\"1\",\"infocode\":\"10000\",\"route\":{\"origin\":\"" + origin
                + "\",\"destination\":\"" + destination + "\",\"paths\":" + paths + "}}";
    }
}
