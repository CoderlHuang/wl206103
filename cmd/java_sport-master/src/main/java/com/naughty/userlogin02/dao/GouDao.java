package com.naughty.userlogin02.dao;

import com.naughty.userlogin02.bean.Gou;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GouDao {
    @Insert("Insert into gou(type,value,time,)values(#{type},#{value},#{time}")
    public int addGou(Gou gou);
}
