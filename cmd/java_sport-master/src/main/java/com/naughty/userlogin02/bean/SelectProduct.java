package com.naughty.userlogin02.bean;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;

import com.aliyuncs.iot.model.v20180120.*;

import com.google.gson.Gson;
import org.springframework.web.bind.annotation.ResponseBody;



@ResponseBody
public class SelectProduct {
    public static JSONObject  selectProduct() {

        DefaultProfile profile = DefaultProfile.getProfile("cn-shanghai", "LTAI5tSmwx4TFAs9wpxncv3D", "FUknOFZM7xEK29YsyLurLbtjNYGUvn");

        /** use STS Token
         DefaultProfile profile = DefaultProfile.getProfile(
         "<your-region-id>",           // The region ID
         "<your-access-key-id>",       // The AccessKey ID of the RAM account
         "<your-access-key-secret>",   // The AccessKey Secret of the RAM account
         "<your-sts-token>");          // STS Token
         **/
        IAcsClient client = new DefaultAcsClient(profile);

        QueryProductListRequest request = new QueryProductListRequest();
        request.setPageSize(3);
        request.setResourceGroupId("rg-acfmzw43h3tcipa");
        request.setCurrentPage(1);
        request.setIotInstanceId("iot-06z00futcg4oenw");
        JSONObject jsonObjecta = new JSONObject();
        try {
            QueryProductListResponse response = client.getAcsResponse(request);
              String st = new Gson().toJson(response);

            JSONObject jsonObject = JSONObject.parseObject(st);

            System.out.println("json"+  jsonObject);
            System.out.println(new Gson().toJson(response));

            return jsonObject;

        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            System.out.println("ErrCode:" + e.getErrCode());
            System.out.println("ErrMsg:" + e.getErrMsg());
            System.out.println("RequestId:" + e.getRequestId());

        }
        return jsonObjecta;

    }
}



