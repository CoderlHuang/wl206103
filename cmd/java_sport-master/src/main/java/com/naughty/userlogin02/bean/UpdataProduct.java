package com.naughty.userlogin02.bean;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;

import com.aliyuncs.iot.model.v20180120.*;
import org.apache.ibatis.annotations.Param;

public class UpdataProduct {
    public static  void  updataproduct(@Param("str") String productname ,@Param("oldstr=") String oldproductname){
        System.err.println(productname);
        System.err.println(oldproductname);
        System.err.println("up"+productname+"old"+oldproductname);
        DefaultProfile profile = DefaultProfile.getProfile("cn-shanghai", "LTAI5tSmwx4TFAs9wpxncv3D", "FUknOFZM7xEK29YsyLurLbtjNYGUvn");

        /** use STS Token
         DefaultProfile profile = DefaultProfile.getProfile(
         "<your-region-id>",           // The region ID
         "<your-access-key-id>",       // The AccessKey ID of the RAM account
         "<your-access-key-secret>",   // The AccessKey Secret of the RAM account
         "<your-sts-token>");          // STS Token
         **/
        IAcsClient client = new DefaultAcsClient(profile);

        UpdateProductRequest request = new UpdateProductRequest();
        request.setIotInstanceId("iot-06z00futcg4oenw");
        request.setProductKey(oldproductname);
        request.setProductName(productname);

        try {
            UpdateProductResponse response = client.getAcsResponse(request);
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

