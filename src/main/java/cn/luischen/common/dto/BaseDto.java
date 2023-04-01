package cn.luischen.common.dto;

import java.io.Serializable;

/**
 * 公共属性的类
 * Created by winterchen on 2018/4/29.
 */
public class BaseDto implements Serializable {


    /** 用户名 */
    private String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
