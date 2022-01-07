package com.naughty.userlogin02.bean;

import java.util.List;

public class MainMenu {
    private int id;
    private String title;
    private String path;
    List<SubMenu> slist;

    public MainMenu() {
    }

    public MainMenu(int id, String title, String path, List<SubMenu> slist) {
        this.id = id;
        this.title = title;
        this.path = path;
        this.slist = slist;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public List<SubMenu> getSlist() {
        return slist;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSlist(List<SubMenu> slist) {
        this.slist = slist;
    }

    @Override
    public String toString() {
        return "MainMenu{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", path='" + path + '\'' +
                ", slist=" + slist +
                '}';
    }
}
