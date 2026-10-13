package com.sky.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.sky.entity.AddressBook;
import com.sky.exception.OrderBusinessException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Builds geocoding addresses and validates geocoding and driving-route responses
 */
final class DeliveryLocationValidator {

    private static final Pattern PROVINCE_CODE = Pattern.compile("\\d{2}");
    private static final Pattern CITY_CODE = Pattern.compile("\\d{4}");
    private static final Pattern DISTRICT_CODE = Pattern.compile("\\d{6}");
    private static final Pattern AD_CODE = Pattern.compile("\\d{6}");
    private static final Pattern ADDRESS_NUMBER = Pattern.compile("\\d+号");
    private static final List<String> MUNICIPALITY_CODES = Arrays.asList("11", "12", "31", "50");
    private static final List<String> PRECISE_LEVELS = Arrays.asList("门牌号", "门址", "单元号", "楼层", "房间");
    private static final List<String> NAMED_PLACE_LEVELS = Arrays.asList("兴趣点", "住宅区");
    private static final Pattern OMITTABLE_SUBADDRESS = Pattern.compile("(.+?)(?:\\d+单元.*|\\d+层.*|\\d+楼.*|\\d+室.*|\\d+房.*)$");
    private static final List<String> PLACEHOLDER_NAMES = Arrays.asList("市辖区", "县", "请选择", "请选择省份", "请选择城市", "请选择区县");

    /**
     * Prevents instantiation of this validation utility
     */
    private DeliveryLocationValidator() {
    }

    /**
     * Builds the Chinese address sent to AMap from the original address-book fields
     *
     * @param addressBook User's structured delivery address
     * @return Chinese address composed of province, city, district, and address details
     * @throws OrderBusinessException If a required address field is missing or administrative codes are inconsistent
     */
    static String buildGeocodingAddress(AddressBook addressBook) {
        if (addressBook == null) {
            throw invalidDeliveryAddress();
        }

        String provinceCode = trim(addressBook.getProvinceCode());
        String cityCode = trim(addressBook.getCityCode());
        String districtCode = trim(addressBook.getDistrictCode());
        String provinceName = trim(addressBook.getProvinceName());
        String cityName = trim(addressBook.getCityName());
        String districtName = trim(addressBook.getDistrictName());
        String detail = addressBook.getDetail();

        if (!PROVINCE_CODE.matcher(provinceCode).matches()
                || !CITY_CODE.matcher(cityCode).matches()
                || !DISTRICT_CODE.matcher(districtCode).matches()
                || !cityCode.startsWith(provinceCode)
                || !districtCode.startsWith(cityCode)
                || !isChineseRegionName(provinceName)
                || !isChineseRegionName(districtName)
                || isPlaceholder(districtName)
                || detail == null
                || detail.trim().isEmpty()
                || containsControlCharacter(detail)) {
            throw invalidDeliveryAddress();
        }

        boolean municipality = MUNICIPALITY_CODES.contains(provinceCode);
        if (municipality && !isExpectedMunicipalityName(provinceCode, provinceName)) {
            throw invalidDeliveryAddress();
        }

        List<String> addressParts = new ArrayList<>();
        addressParts.add(provinceName);

        if (municipality && (cityName.isEmpty() || isPlaceholder(cityName) || cityName.equals(provinceName))) {
            // Municipality-level district or county labels are placeholders; the province name already identifies the city
        } else {
            if (!isChineseRegionName(cityName) || isPlaceholder(cityName)) {
                throw invalidDeliveryAddress();
            }
            if (!cityName.equals(provinceName)) {
                addressParts.add(cityName);
            }
        }

        addressParts.add(districtName);
        addressParts.add(detail);
        return String.join("", addressParts);
    }

    /**
     * Selects the unique geocoding result that matches the district and has sufficient address precision
     *
     * @param responseBody AMap JSON response
     * @param requestedDetail Original address details entered by the user
     * @param expectedDistrictCode Selected district code, or {@code null} for the merchant address
     * @param addressLabel Address label used in business errors
     * @return Longitude and latitude returned by AMap
     * @throws OrderBusinessException If the service fails, the match is imprecise, or results are ambiguous
     */
    static String resolveGeocodeCoordinate(
            String responseBody,
            String requestedDetail,
            String expectedDistrictCode,
            String addressLabel) {
        JSONObject response = parseResponse(responseBody, addressLabel + "定位服务暂不可用");
        if (!isSuccessfulResponse(response)) {
            throw new OrderBusinessException(addressLabel + "定位服务暂不可用");
        }

        Object rawGeocodes = response.get("geocodes");
        JSONArray geocodes = rawGeocodes instanceof JSONArray ? (JSONArray) rawGeocodes : null;
        if (geocodes == null || geocodes.isEmpty()) {
            throw inaccurateGeocode(addressLabel);
        }
        if (!hasConsistentCount(response.getString("count"), geocodes.size())) {
            throw inaccurateGeocode(addressLabel);
        }

        String expectedDetail = normalizeAddressPart(requestedDetail);
        String normalizedDistrictCode = expectedDistrictCode == null ? null : expectedDistrictCode.trim();
        List<String> acceptableCoordinates = new ArrayList<>();
        for (int index = 0; index < geocodes.size(); index++) {
            Object rawCandidate = geocodes.get(index);
            if (!(rawCandidate instanceof JSONObject)) {
                continue;
            }
            JSONObject candidate = (JSONObject) rawCandidate;
            String adcode = candidate.getString("adcode");
            String level = candidate.getString("level");
            String formattedAddress = normalizeAddressPart(candidate.getString("formatted_address"));
            String location = candidate.getString("location");
            if (!AD_CODE.matcher(valueOrEmpty(adcode)).matches()
                    || (normalizedDistrictCode != null && !normalizedDistrictCode.equals(adcode))
                    || !isAcceptableLevel(level, expectedDetail, formattedAddress)
                    || !matchesRequestedDetail(expectedDetail, formattedAddress, candidate)
                    || !isValidCoordinate(location)) {
                continue;
            }
            addDistinctCoordinate(acceptableCoordinates, location.trim());
        }

        if (acceptableCoordinates.size() != 1) {
            throw inaccurateGeocode(addressLabel);
        }
        return acceptableCoordinates.get(0);
    }

    /**
     * Validates the AMap route response, echoed endpoints, and distance of the first recommended route
     *
     * @param responseBody AMap driving-route JSON response
     * @param expectedOrigin Requested origin coordinates
     * @param expectedDestination Requested destination coordinates
     * @return Distance of the first recommended route, in meters
     * @throws OrderBusinessException If the route is unavailable, endpoints do not match, or distance is invalid
     */
    static BigDecimal resolveDrivingDistance(String responseBody, String expectedOrigin, String expectedDestination) {
        JSONObject response = parseResponse(responseBody, "配送路线服务暂不可用");
        if (!isSuccessfulResponse(response)) {
            throw new OrderBusinessException("配送路线服务暂不可用");
        }

        Object rawRoute = response.get("route");
        JSONObject route = rawRoute instanceof JSONObject ? (JSONObject) rawRoute : null;
        if (route == null
                || !sameCoordinate(route.getString("origin"), expectedOrigin)
                || !sameCoordinate(route.getString("destination"), expectedDestination)) {
            throw new OrderBusinessException("配送路线结果无效，请稍后重试");
        }

        Object rawPaths = route.get("paths");
        JSONArray paths = rawPaths instanceof JSONArray ? (JSONArray) rawPaths : null;
        if (paths == null || paths.isEmpty() || !hasConsistentCount(response.getString("count"), paths.size())
                || !(paths.get(0) instanceof JSONObject)) {
            throw new OrderBusinessException("配送路线暂时无法规划");
        }

        String distanceText = ((JSONObject) paths.get(0)).getString("distance");
        try {
            BigDecimal distance = new BigDecimal(distanceText);
            if (distance.signum() < 0) {
                throw new NumberFormatException("negative distance");
            }
            return distance;
        } catch (RuntimeException exception) {
            throw new OrderBusinessException("配送路线结果无效，请稍后重试");
        }
    }

    /**
     * Applies the existing five-kilometer driving-distance limit
     *
     * @param distance Driving distance returned by AMap, in meters
     * @throws OrderBusinessException If the driving distance exceeds five kilometers
     */
    static void requireWithinDeliveryRange(BigDecimal distance) {
        if (distance == null || distance.signum() < 0) {
            throw new OrderBusinessException("配送路线结果无效，请稍后重试");
        }
        if (distance.compareTo(BigDecimal.valueOf(5000)) > 0) {
            throw new OrderBusinessException("超出配送范围");
        }
    }

    /**
     * Safely parses an AMap JSON response
     *
     * @param responseBody HTTP response body
     * @param errorMessage Business error to raise when parsing fails
     * @return Parsed JSON object
     * @throws OrderBusinessException If the response is empty, malformed, or has an invalid root object
     */
    private static JSONObject parseResponse(String responseBody, String errorMessage) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new OrderBusinessException(errorMessage);
        }
        try {
            JSONObject response = JSONObject.parseObject(responseBody);
            if (response == null) {
                throw new OrderBusinessException(errorMessage);
            }
            return response;
        } catch (JSONException exception) {
            throw new OrderBusinessException(errorMessage);
        }
    }

    /**
     * Checks the AMap success status and validates infocode when provided
     *
     * @param response AMap response object
     * @return {@code true} when the response status is valid
     */
    private static boolean isSuccessfulResponse(JSONObject response) {
        String infocode = response.getString("infocode");
        return "1".equals(response.getString("status"))
                && (infocode == null || infocode.trim().isEmpty() || "10000".equals(infocode));
    }

    /**
     * Checks that the optional result count matches the returned list
     *
     * @param countText Result count reported by the service
     * @param actualSize Actual list size
     * @return {@code true} when count is absent or matches the list size
     */
    private static boolean hasConsistentCount(String countText, int actualSize) {
        if (countText == null || countText.trim().isEmpty()) {
            return true;
        }
        try {
            return Integer.parseInt(countText) == actualSize;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * Determines whether the AMap match level identifies a specific location
     *
     * @param level AMap match level
     * @param expectedDetail Normalized address details; empty for the merchant address
     * @param formattedAddress Normalized formatted address
     * @return {@code true} when the match level is sufficient for this address type
     */
    private static boolean isAcceptableLevel(String level, String expectedDetail, String formattedAddress) {
        if (PRECISE_LEVELS.contains(level)) {
            return true;
        }
        // Accept a POI or residential area only when the result includes the specific place entered by the user; an administrative area or business-district center is not a delivery point
        return NAMED_PLACE_LEVELS.contains(level) && !expectedDetail.isEmpty() && formattedAddress.contains(expectedDetail);
    }

    /**
     * Checks that the returned address matches the user input while allowing AMap to omit a trailing floor, unit, or room
     *
     * @param expectedDetail Normalized user address details
     * @param formattedAddress Normalized AMap address
     * @param candidate Geocoding candidate
     * @return {@code true} when the address matches or only a trailing indoor detail is omitted
     */
    private static boolean matchesRequestedDetail(String expectedDetail, String formattedAddress, JSONObject candidate) {
        if (expectedDetail.isEmpty()) {
            return true;
        }
        if (!formattedAddress.isEmpty() && formattedAddress.contains(expectedDetail)) {
            return true;
        }
        String baseAddress = expectedDetail;
        java.util.regex.Matcher matcher = OMITTABLE_SUBADDRESS.matcher(expectedDetail);
        if (matcher.matches()) {
            baseAddress = matcher.group(1);
        }
        if (!formattedAddress.isEmpty() && baseAddress.length() >= 4 && formattedAddress.contains(baseAddress)) {
            return structuredStreetAndNumberConsistent(baseAddress, candidate, false);
        }
        java.util.regex.Matcher expectedNumberMatcher = ADDRESS_NUMBER.matcher(baseAddress);
        if (expectedNumberMatcher.find()) {
            String expectedNumber = normalizeAddressPart(expectedNumberMatcher.group());
            String returnedNumber = normalizeAddressPart(candidate.getString("number"));
            if (returnedNumber.isEmpty() || !baseAddress.contains(returnedNumber)
                    || !returnedNumber.contains(expectedNumber)) {
                return false;
            }
        }
        // formatted_address is not listed in the current official geocoding fields; when it is absent, rely only on documented street and number fields
        return structuredStreetAndNumberConsistent(baseAddress, candidate, true);
    }

    /**
     * Matches the input against road and street-number fields in the AMap geocoding result
     *
     * @param baseAddress User address details, optionally without trailing indoor details
     * @param candidate AMap candidate
     * @param requireField Whether at least one structured street or number field must be present
     * @return {@code true} when returned fields match the input and include a road or street number
     */
    private static boolean structuredStreetAndNumberConsistent(String baseAddress, JSONObject candidate, boolean requireField) {
        String street = normalizeAddressPart(candidate.getString("street"));
        String number = normalizeAddressPart(candidate.getString("number"));
        return (!requireField || !street.isEmpty() || !number.isEmpty())
                && (street.isEmpty() || baseAddress.contains(street))
                && (number.isEmpty() || baseAddress.contains(number));
    }

    /**
     * Deduplicates identical coordinates while retaining distinct locations for ambiguity checks
     *
     * @param coordinates Coordinates collected so far
     * @param candidate Candidate coordinate
     */
    private static void addDistinctCoordinate(List<String> coordinates, String candidate) {
        for (String coordinate : coordinates) {
            if (sameCoordinate(coordinate, candidate)) {
                return;
            }
        }
        coordinates.add(candidate);
    }

    /**
     * Validates coordinate format and longitude/latitude bounds
     *
     * @param location Longitude and latitude returned by AMap
     * @return {@code true} when the coordinate is valid
     */
    private static boolean isValidCoordinate(String location) {
        if (location == null) {
            return false;
        }
        String[] parts = location.split(",", -1);
        if (parts.length != 2) {
            return false;
        }
        try {
            double longitude = Double.parseDouble(parts[0].trim());
            double latitude = Double.parseDouble(parts[1].trim());
            return Double.isFinite(longitude)
                    && Double.isFinite(latitude)
                    && longitude >= -180 && longitude <= 180
                    && latitude >= -90 && latitude <= 90
                    && !(longitude == 0 && latitude == 0);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * Compares route-response coordinates with the requested coordinates
     *
     * @param actual Coordinates echoed by AMap
     * @param expected Coordinates from this request
     * @return {@code true} when the coordinates match to six decimal places
     */
    private static boolean sameCoordinate(String actual, String expected) {
        if (!isValidCoordinate(actual) || !isValidCoordinate(expected)) {
            return false;
        }
        String[] actualParts = actual.split(",");
        String[] expectedParts = expected.split(",");
        return Math.abs(Double.parseDouble(actualParts[0].trim()) - Double.parseDouble(expectedParts[0].trim())) <= 0.000001
                && Math.abs(Double.parseDouble(actualParts[1].trim()) - Double.parseDouble(expectedParts[1].trim())) <= 0.000001;
    }

    /**
     * Checks whether an administrative-area name contains Chinese characters
     *
     * @param value Administrative-area name
     * @return {@code true} when the name contains Chinese characters
     */
    private static boolean isChineseRegionName(String value) {
        return value != null && value.matches(".*\\p{IsHan}+.*");
    }

    /**
     * Checks whether a name is an address-picker placeholder
     *
     * @param value Administrative-area name
     * @return {@code true} when the name is empty or a placeholder
     */
    private static boolean isPlaceholder(String value) {
        return value == null || value.isEmpty() || PLACEHOLDER_NAMES.contains(value);
    }

    /**
     * Checks that a municipality code matches its province-level name
     *
     * @param provinceCode Province-level administrative code
     * @param provinceName Province-level administrative name
     * @return {@code true} when the code and name match
     */
    private static boolean isExpectedMunicipalityName(String provinceCode, String provinceName) {
        switch (provinceCode) {
            case "11": return "北京市".equals(provinceName);
            case "12": return "天津市".equals(provinceName);
            case "31": return "上海市".equals(provinceName);
            case "50": return "重庆市".equals(provinceName);
            default: return false;
        }
    }

    /**
     * Normalizes address text for matching without changing the text sent to AMap
     *
     * @param value Address field
     * @return Comparison text with whitespace removed
     */
    private static String normalizeAddressPart(String value) {
        return value == null ? "" : value.replaceAll("[\\s，。,.、:：;；（）()【】\\[\\]·]", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Checks whether an address field contains control characters
     *
     * @param value Address field
     * @return {@code true} when control characters are present
     */
    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(Character::isISOControl);
    }

    /**
     * Creates the standard invalid-address exception
     *
     * @return Standard address error
     */
    private static OrderBusinessException invalidDeliveryAddress() {
        return new OrderBusinessException("收货地址行政区划或详细地址无效，请重新选择并补充地址");
    }

    /**
     * Creates the standard insufficient-precision exception
     *
     * @param addressLabel Address label
     * @return Standard precise-location error
     */
    private static OrderBusinessException inaccurateGeocode(String addressLabel) {
        return new OrderBusinessException(addressLabel + "无法准确定位，请补充完整门牌地址");
    }

    /**
     * Converts a null string to an empty string
     *
     * @param value Original text
     * @return Non-null text or an empty string
     */
    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Trims surrounding whitespace from an address field
     *
     * @param value Original field
     * @return Field with surrounding whitespace removed
     */
    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
