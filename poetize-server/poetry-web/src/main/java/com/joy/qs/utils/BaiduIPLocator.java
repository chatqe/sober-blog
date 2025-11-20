package com.joy.qs.utils;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    private String AK ;

    @Value("${baidu.url.ip-location}")
    private String IP_LOCATION_URL ;
    /**
     *
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
    public  String getLocationByIp(String ip) {
        String urlString = IP_LOCATION_URL + "?ip=" + ip+"&coor=" + COOR +   "&ak=" + AK;
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
                System.out.println("请求失败，HTTP 错误代码: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    /**
     * 解析返回的 JSON 数据，获取具体位置信息
     *
     * @param jsonResponse 返回的 JSON 字符串
     * @return 位置相关的信息
     */
    public  String parseLocationInfo(String jsonResponse) {
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

    // public static void main(String[] args) {
    //     String ip = "111.60.23.2";  // 示例 IP，可以替换成任何你想查询的 IP
    //     String response = getLocationByIp(ip);
    //     System.out.println("API 响应: " + response);
    //     System.out.println("位置： " + parseLocationInfo(response));
    // }


}
