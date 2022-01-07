package com.naughty.userlogin02;

import com.naughty.userlogin02.bean.MainMenu;
import com.naughty.userlogin02.bean.User;
//import com.naughty.userlogin02.dao.MenuDao;
import com.naughty.userlogin02.dao.MenuDao;
import com.naughty.userlogin02.dao.UserDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class Userlogin02ApplicationTests {

    @Autowired
    UserDao userDao;
    @Autowired
    MenuDao menuDao;

    @Test
    void contextLoads() {
//        List<User> ulist = userDao.getAllUser(null,0,2);
//        System.out.println("总人数为: "+ulist.size());
//        for (User us:ulist
//             ) {
//            System.out.println(us);
//        }

        List<MainMenu> mainMenus = menuDao.getMainMenus();
        for (MainMenu mm:mainMenus) {
            System.out.println(mm);
        }
    }

}
