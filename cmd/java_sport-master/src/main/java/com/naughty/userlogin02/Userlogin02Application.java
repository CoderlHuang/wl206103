package com.naughty.userlogin02;

import com.naughty.userlogin02.controller.ampq;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan("com.naughty.userlogin02.dao")
@SpringBootApplication
public class Userlogin02Application {

    public static void main(String[] args) {
        SpringApplication.run(Userlogin02Application.class, args);
        try {
            ampq.mystart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
