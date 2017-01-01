package com.example.imageloadetest.Bean;

/**
 * Created by U310 on 2017/1/1.
 */

public class FolderBean {
    private String dir;
    private String firstImgDir;
    private String name;
    private int count;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastIndex =this. dir.lastIndexOf("/");
        this.name = this.dir.substring(lastIndex);
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getName() {
        return name;
    }


    public String getFirstImgDir() {
        return firstImgDir;
    }

    public void setFirstImgDir(String firstImgDir) {
        this.firstImgDir = firstImgDir;
    }
}
