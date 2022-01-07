package com.naughty.userlogin02.bean;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;

import com.aliyuncs.iot.model.v20180120.*;


public class DeleteProduct {

    public static void delete(String  ProductKey ) {    //输入产品key
        System.err.println("ProductKey"+ProductKey);
        DefaultProfile profile = DefaultProfile.getProfile("cn-shanghai", "LTAI5tSmwx4TFAs9wpxncv3D", "FUknOFZM7xEK29YsyLurLbtjNYGUvn");
        IAcsClient client = new DefaultAcsClient(profile);

        DeleteProductRequest request = new DeleteProductRequest();
        request.setProductKey(ProductKey);
        request.setIotInstanceId("iot-06z00futcg4oenw");

        try {
            DeleteProductResponse response = client.getAcsResponse(request);
            System.out.println(new Gson().toJson(response));
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            System.out.println("ErrCode:" + e.getErrCode());
            System.out.println("ErrMsg:" + e.getErrMsg());
            System.out.println("RequestId:" + e.getRequestId());
        }

    }
}
