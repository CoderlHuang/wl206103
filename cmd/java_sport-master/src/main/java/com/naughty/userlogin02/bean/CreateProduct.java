//package com.naughty.userlogin02.bean;
//
//// This file is auto-generated, don't edit it. Thanks.
//
//import com.aliyun.iot20180120.Client;
//import com.aliyun.tea.*;
//import com.aliyun.iot20180120.*;
//import com.aliyun.iot20180120.models.*;
//import com.aliyun.teaopenapi.*;
//import com.aliyun.teaopenapi.models.*;
//
//public class CreateProduct {
//
//    /**
//     * 使用AK&SK初始化账号Client
//     * @param accessKeyId
//     * @param accessKeySecret
//     * @return Client
//     * @throws Exception
//     */
//    public static Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
//        System.err.println(accessKeyId);
//        System.err.println(accessKeySecret);
//        Config config = new Config()
//                // 您的AccessKey ID
//                .setAccessKeyId("LTAI5tSmwx4TFAs9wpxncv3D")
//                // 您的AccessKey Secret
//                .setAccessKeySecret("FUknOFZM7xEK29YsyLurLbtjNYGUvn");
//        // 访问的域名
//        config.endpoint = "iot.cn-shanghai.aliyuncs.com";
//        return new Client(config);
//    }
//
//    public void create(String devicename,String a,String b) throws Exception {
////        java.util.List<String> args = java.util.Arrays.asList(args_);
//        System.err.println(a);
//        System.err.println(b);
//        Client client = CreateProduct.createClient(a,b);
//        CreateProductRequest createProductRequest = new CreateProductRequest()
//                .setIotInstanceId("iot-06z00futcg4oenw")
//                .setProductName(devicename)
//                .setNodeType(1);
//        // 复制代码运行请自行打印 API 的返回值
//        client.createProduct(createProductRequest);
//    }
//}
