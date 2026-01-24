package io.charqe.qs.infra.thirdparty.baidumap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 百度地图IP定位响应数据对象
 *
 * @author Joy
 * @since 2025-07-13 17:20
 */
@Data
public class BaiduIPResult {

    /**
     * 状态码，0表示成功，其他表示失败
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String message;
    /**
     * 格式：CN|安徽省|合肥市|经开区|None|100|89|88
     */
    private String address;
    /**
     * 响应内容
     */
    private Content content;

    /**
     * 响应内容内部类
     */
    @Data
    public static class Content {

        /**
         * 地址信息
         */
        private Address address;

        /**
         * 地址详情
         */
        @JsonProperty("address_detail")
        private AddressDetail addressDetail;

        /**
         * 坐标信息
         */
        private Point point;

        /**
         * 地址信息内部类
         */
        @Data
        public static class Address {
            /**
             * 国家
             */
            private String country;

            /**
             * 省
             */
            private String province;

            /**
             * 市
             */
            private String city;

            /**
             * 区
             */
            private String district;

            /**
             * 街道
             */
            private String street;

            /**
             * 门牌号
             */
            @JsonProperty("street_number")
            private String streetNumber;

            /**
             * 行政区划代码
             */
            @JsonProperty("adcode")
            private String adCode;
        }

        /**
         * 地址详情内部类
         */
        @Data
        public static class AddressDetail {
            /**
             * 国家代码
             */
            private String country_code;

            /**
             * 省
             */
            private String province;

            /**
             * 城市
             */
            private String city;

            /**
             * 区
             */
            private String district;

            /**
             * 街道
             */
            private String street;

            /**
             * 门牌号
             */
            private String street_number;

            /**
             * 行政区划代码
             */
            private String adcode;

            /**
             * 城市代码
             */
            private String city_code;
        }

        /**
         * 坐标信息内部类
         */
        @Data
        public static class Point {
            /**
             * 经度
             */
            private Double x;

            /**
             * 纬度
             */
            private Double y;
        }
    }
}