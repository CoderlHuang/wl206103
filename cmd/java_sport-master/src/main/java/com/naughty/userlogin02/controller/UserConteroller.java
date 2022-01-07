package com.naughty.userlogin02.controller;

import com.alibaba.fastjson.JSON;
import com.naughty.userlogin02.bean.*;
import com.naughty.userlogin02.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.jms.MessageListener;
import java.util.HashMap;
import java.util.List;

//此处为用户及设备增删改查Controller类
@RestController
public class UserConteroller {
//    CreateProduct createProduct;
    @Autowired
    UserDao userDao;

    @CrossOrigin
    @RequestMapping("/allUser")
    public String getUserList(QueryInfo queryInfo){
        System.out.println(queryInfo);
        int numbers = userDao.getUserCounts("%"+queryInfo.getQuery()+"%");// 获取数据总数
        int pageStart = (queryInfo.getPageNum()-1)*queryInfo.getPageSize();
        List<User> users = userDao.getAllUser("%"+queryInfo.getQuery()+"%",pageStart,queryInfo.getPageSize());
        HashMap<String, Object> res = new HashMap<>();
        res.put("numbers",numbers);
        res.put("data",users);
        System.out.println("总条数："+numbers);
        String users_json = JSON.toJSONString(res);
        return users_json;
    }

    @RequestMapping("/userState")
    public String updateUserState(@RequestParam("id") Integer  id,
                                  @RequestParam("state") Boolean state){
        int i = userDao.updateState(id, state);
        System.out.println("用户编号:"+id);
        System.out.println("用户状态:"+state);
        String str = i >0?"success":"error";
        return str;
    }

    @RequestMapping("/addUser")
    public String addUser(@RequestBody User user){
        System.out.println(user);
        user.setRole("普通用户");
        user.setState(false);
        int i = userDao.addUser(user);
        String str = i >0?"success":"error";
        return str;
    }

    @RequestMapping("/getUpdate")
    public String getUpdateUser(int id){
        System.out.println("编号:"+id);
        User updateUser = userDao.getUpdateUser(id);
        String users_json = JSON.toJSONString(updateUser);
        return users_json;
    }
    @RequestMapping("/update")
    public String getUpdatepro(String str,String oldstr){
        UpdataProduct updataProduct = new UpdataProduct();
        updataProduct.updataproduct(str,oldstr);
        System.out.println(str+oldstr);
        return "我是大帅哥";
    }

    @RequestMapping("/select")
    public JSON select(){
        SelectProduct selectProduct = new SelectProduct();
      return   selectProduct.selectProduct();


   }
    @RequestMapping("/delete")
    public String   delete( String delestr){
        DeleteProduct deleteProduct = new DeleteProduct();
        deleteProduct.delete(delestr);
        System.out.println(delestr);
            return "删除成功";

    }
//    @RequestMapping("/create")
//    public String create1(String str) throws Exception {
//        createProduct.create(str,"LTAI5tSmwx4TFAs9wpxncv3D","FUknOFZM7xEK29YsyLurLbtjNYGUvn");
//        System.out.println(str);
//        return "增加成功";
//    }

    @RequestMapping("/editUser")
    public String editUser(@RequestBody User user){
        System.out.println(user);
        int i = userDao.editUser(user);
        String str = i >0?"success":"error";
        return str;
    }

    @RequestMapping("/deleteUser")
    public String deleteUser(int id){
        System.out.println(id);
        int i = userDao.deleteUser(id);
        String str = i >0?"success":"error";
        return str;
    }
}
