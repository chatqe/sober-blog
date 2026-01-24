package io.charqe.qs.infra.thirdparty.baidumap;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.infra.thirdparty.baidumap.dto.AccessLogConvertDTO;
import io.charqe.qs.infra.thirdparty.baidumap.dto.BaiduIPResult;
import io.charqe.qs.infra.thirdparty.baidumap.enums.CountryEnums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 百度普通定位相关工具类
 *
 * @author sjq
 * @since 2025-07-13 17:20
 */
@Slf4j
@Component
public class BaiduIPLocator {


    @Value("${baidu.ak}")
    private String AK;

    @Value("${baidu.url.ip-location}")
    private String IP_LOCATION_URL;

    private static String ipLocationUrl;
    private static String ak;

    /**
     * 初始化时赋值到静态变量
     */
    @PostConstruct
    public void init() {
        ak = this.AK;
        ipLocationUrl = this.IP_LOCATION_URL;
    }

    /**
     * 设置返回位置信息中，经纬度的坐标类型，分别如下：
     * coor不出现、或为空：百度墨卡托坐标，即百度米制坐标
     * coor = bd09ll：百度经纬度坐标，在国测局坐标基础之上二次加密而来
     * coor = gcj02：国测局02坐标，在原始GPS坐标基础上，按照国家测绘行业统一要求，加密后的坐标
     */
    private static final String COOR = "bd09ll";


    /**
     * 根据 IP 获取地理位置信息
     *
     * @param ip 地址
     * @return 位置信息的 JSON 格式字符串
     */
    public static String getLocationJsonByIp(String ip) {
        String urlString = ipLocationUrl + "?ip=" + ip + "&coor=" + COOR + "&ak=" + ak;
        return sendRequest(urlString);
    }

    /**
     * 发送 HTTP 请求并返回响应
     *
     * @param urlString 请求的 URL
     * @return 响应内容
     */
    private static String sendRequest(String urlString) {
        StringBuilder result = new StringBuilder();
        try {
            // 创建 URL 对象
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置请求方式
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // 获取响应代码
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取响应
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    result.append(inputLine);
                }
                in.close();
            } else {
                log.error("BaiduIPLocator-sendRequest 请求失败，HTTP 错误代码: " + responseCode);
            }
        } catch (Exception e) {
            log.error("BaiduIPLocator-sendRequest 请求失败：", e);
            e.printStackTrace();
        }
        return result.toString();
    }

    /**
     * 使用FastJSON2解析百度IP定位JSON，映射到accessLog实体
     *
     * @param ip 待解析IP
     * @return 访问日志转换DTO
     */
    public static AccessLogConvertDTO getResultDTO(String ip) {

        String baiduIpJson = getLocationJsonByIp(ip);
        AccessLogConvertDTO convertDTO = new AccessLogConvertDTO();

        // 空值兜底：JSON为空直接返回
        if (StringUtils.isBlank(baiduIpJson)) {
            log.warn("BaiduIPLocator-getResultDTO 百度IP定位JSON为空，跳过解析");
            return initDefaultValue(convertDTO);
        }

        try {
            // 核心：FastJSON2的解析API（一行完成，比Jackson更简洁）
            BaiduIPResult ipResult = JSON.parseObject(baiduIpJson, BaiduIPResult.class);

            // 校验API调用状态（0为成功）
            if (ipResult.getStatus() == null || ipResult.getStatus() != 0) {
                log.warn("BaiduIPLocator-getResultDTO 百度IP定位API调用失败，status={}", ipResult.getStatus());
                return initDefaultValue(convertDTO);
            }

            // 逐层提取字段，做空值兜底
            BaiduIPResult.Content content = ipResult.getContent();
            if (content == null) {
                log.warn("BaiduIPLocator-getResultDTO 百度IP定位返回content为空");
                return initDefaultValue(convertDTO);

            }

            BaiduIPResult.Content.AddressDetail detail = content.getAddressDetail();
            if (detail == null) {
                log.warn("BaiduIPLocator-getResultDTO 百度IP定位返回address_detail为空");
                return initDefaultValue(convertDTO);

            }

            // 映射到accessLog字段（标准化+空值兜底）
            String countryCode = ipResult.getAddress().substring(0, 2);
            convertDTO.setCountry(StringUtils.defaultString(CountryEnums.getNameByCode(countryCode), ""));
            convertDTO.setProvince(StringUtils.defaultString(detail.getProvince(), ""));
            convertDTO.setCity(StringUtils.defaultString(detail.getCity(), ""));
            convertDTO.setDistrict(StringUtils.defaultString(detail.getDistrict(), ""));

            return convertDTO;

        } catch (Exception e) {
            // 捕获解析异常，记录日志并兜底赋值
            log.error("BaiduIPLocator-getResultDTO FastJSON2解析百度IP定位JSON失败，json={}", baiduIpJson, e);
            return initDefaultValue(convertDTO);
        }
    }

    /**
     * 获取具体经纬度
     *
     * @param jsonResponse 返回的 JSON 字符串
     * @return 位置相关的信息
     */
    public String parseGeoCoordinates(String jsonResponse) {
        try {
            // 解析 JSON 数据
            JSONObject jsonObject = new JSONObject(jsonResponse);
            if (jsonObject.getInt("status") == 0) {
                // 获取位置字段
                JSONObject content = jsonObject.getJSONObject("content");
                JSONObject point = content.getJSONObject("point");
                double longitude = point.getDouble("x"); // 经度
                double latitude = point.getDouble("y"); // 纬度
                return "经度: " + longitude + ", 纬度: " + latitude;
            } else {
                return "定位失败，错误信息：" + jsonObject.getString("message");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "解析失败";
        }
    }

    /**
     * 统一兜底赋值方法（抽离复用，符合代码规范）
     */
    private static AccessLogConvertDTO initDefaultValue(AccessLogConvertDTO dto) {
        dto.setCountry("未知");
        dto.setProvince("");
        dto.setCity("");
        dto.setDistrict("");

        return dto;
    }
    // public static void main(String[] args) {
    //     String ip = "111.60.23.2";  // 示例 IP，可以替换成任何你想查询的 IP
    //     String response = getLocationByIp(ip);
    //     System.out.println("API 响应: " + response);
    //     System.out.println("位置： " + parseLocationInfo(response));
    // }


}


