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
 * 构造高德地理编码地址并校验定位及驾车路线响应
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
     * 禁止实例化纯校验工具
     */
    private DeliveryLocationValidator() {
    }

    /**
     * 根据地址簿原始中文行政区划字段生成供高德查询的完整地址
     *
     * @param addressBook 用户保存的结构化收货地址
     * @return 省、市、区县及详细地址组成的中文地址
     * @throws OrderBusinessException 行政区划或详细地址缺失、代码关系不合法
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
            // 四个直辖市的“市辖区/县”是层级占位值，省级名称已表达城市。
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
     * 从高德地理编码响应中选出唯一、行政区匹配且定位级别足够精确的坐标
     *
     * @param responseBody 高德 JSON 响应
     * @param requestedDetail 用户填写的详细地址原文
     * @param expectedDistrictCode 用户所选区县代码；查询商家地址时传空
     * @param addressLabel 用于业务错误提示的地址名称
     * @return 高德返回的经度、纬度字符串
     * @throws OrderBusinessException 服务响应失败、匹配不准确或结果存在歧义
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
     * 校验高德驾车路线响应、请求起终点回显及首条推荐路线距离
     *
     * @param responseBody 高德驾车路线 JSON 响应
     * @param expectedOrigin 请求的起点坐标
     * @param expectedDestination 请求的终点坐标
     * @return 首条推荐路线的距离，单位为米
     * @throws OrderBusinessException 路线不可用、坐标回显不符或距离无效
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
     * 按现有五公里驾车距离边界判断是否允许配送
     *
     * @param distance 高德返回的驾车距离，单位为米
     * @throws OrderBusinessException 距离超过五公里
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
     * 安全解析高德 JSON 响应
     *
     * @param responseBody HTTP 响应正文
     * @param errorMessage 解析失败时返回的业务错误
     * @return JSON 对象
     * @throws OrderBusinessException 响应为空、格式错误或根对象无效
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
     * 校验高德成功状态，并在服务确实返回 infocode 时检查其成功值
     *
     * @param response 高德响应对象
     * @return 响应状态有效时返回 true
     */
    private static boolean isSuccessfulResponse(JSONObject response) {
        String infocode = response.getString("infocode");
        return "1".equals(response.getString("status"))
                && (infocode == null || infocode.trim().isEmpty() || "10000".equals(infocode));
    }

    /**
     * 校验可选的结果数量字段与实际返回列表一致
     *
     * @param countText 服务返回的结果数量
     * @param actualSize 实际列表长度
     * @return 数量缺省或与列表一致时返回 true
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
     * 判断高德匹配级别是否能提供具体地址位置
     *
     * @param level 高德匹配级别
     * @param expectedDetail 标准化后的用户详细地址，店铺地址可为空
     * @param formattedAddress 标准化后的格式化地址
     * @return 匹配级别满足当前地址用途时返回 true
     */
    private static boolean isAcceptableLevel(String level, String expectedDetail, String formattedAddress) {
        if (PRECISE_LEVELS.contains(level)) {
            return true;
        }
        // POI/住宅区只在结果地址明确包含用户填写的具体地点时接受，避免把行政区或商圈中心点当作收货点。
        return NAMED_PLACE_LEVELS.contains(level) && !expectedDetail.isEmpty() && formattedAddress.contains(expectedDetail);
    }

    /**
     * 校验返回地址是否对应用户输入，并容忍高德省略末尾楼层、单元或房间描述
     *
     * @param expectedDetail 标准化后的用户详细地址
     * @param formattedAddress 标准化后的高德地址
     * @param candidate 地理编码候选
     * @return 地址精确对应或仅省略末级室内描述时返回 true
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
        // formatted_address 未列入当前官方地理编码字段表；缺省时只依赖文档明确列出的 street/number。
        return structuredStreetAndNumberConsistent(baseAddress, candidate, true);
    }

    /**
     * 使用高德地理编码响应中的道路与门牌字段核对输入地址
     *
     * @param baseAddress 用户详细地址或去除末尾室内信息后的基础地址
     * @param candidate 高德候选结果
     * @return 返回字段均能在输入中核对且至少有道路或门牌字段时返回 true
     */
    private static boolean structuredStreetAndNumberConsistent(String baseAddress, JSONObject candidate, boolean requireField) {
        String street = normalizeAddressPart(candidate.getString("street"));
        String number = normalizeAddressPart(candidate.getString("number"));
        return (!requireField || !street.isEmpty() || !number.isEmpty())
                && (street.isEmpty() || baseAddress.contains(street))
                && (number.isEmpty() || baseAddress.contains(number));
    }

    /**
     * 将同一地理位置的重复候选合并，保留不同坐标以便歧义检查
     *
     * @param coordinates 已收集的候选坐标
     * @param candidate 待检查坐标
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
     * 判断地理编码坐标格式及经纬度范围
     *
     * @param location 高德返回的经度、纬度
     * @return 坐标有效时返回 true
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
     * 比较路线响应回显的坐标和本次请求坐标
     *
     * @param actual 高德响应坐标
     * @param expected 本次请求坐标
     * @return 两组坐标在六位小数精度范围内一致时返回 true
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
     * 检查行政区划名称是否为中文名称
     *
     * @param value 行政区划名称
     * @return 名称含中文字符时返回 true
     */
    private static boolean isChineseRegionName(String value) {
        return value != null && value.matches(".*\\p{IsHan}+.*");
    }

    /**
     * 检查名称是否为地址选择器中的占位值
     *
     * @param value 行政区划名称
     * @return 名称为空或占位时返回 true
     */
    private static boolean isPlaceholder(String value) {
        return value == null || value.isEmpty() || PLACEHOLDER_NAMES.contains(value);
    }

    /**
     * 判断直辖市省级代码和名称是否对应
     *
     * @param provinceCode 省级行政区代码
     * @param provinceName 省级行政区名称
     * @return 代码与名称匹配时返回 true
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
     * 标准化地址文本用于比对，不改变实际发送给高德的原文
     *
     * @param value 地址字段
     * @return 去除空白的比较文本
     */
    private static String normalizeAddressPart(String value) {
        return value == null ? "" : value.replaceAll("[\\s，。,.、:：;；（）()【】\\[\\]·]", "").toLowerCase(Locale.ROOT);
    }

    /**
     * 检查地址字段是否包含控制字符
     *
     * @param value 地址字段
     * @return 包含控制字符时返回 true
     */
    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(Character::isISOControl);
    }

    /**
     * 构造地址数据无效的业务异常
     *
     * @return 统一地址错误
     */
    private static OrderBusinessException invalidDeliveryAddress() {
        return new OrderBusinessException("收货地址行政区划或详细地址无效，请重新选择并补充地址");
    }

    /**
     * 构造地址匹配精度不足的业务异常
     *
     * @param addressLabel 地址用途名称
     * @return 统一精确定位错误
     */
    private static OrderBusinessException inaccurateGeocode(String addressLabel) {
        return new OrderBusinessException(addressLabel + "无法准确定位，请补充完整门牌地址");
    }

    /**
     * 将 null 字符串转为空字符串
     *
     * @param value 原始文本
     * @return 非空文本或空字符串
     */
    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 去除地址字段外围空白
     *
     * @param value 原始字段
     * @return 去除外围空白后的字段
     */
    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
