package cn.luischen.common.dto.cond;

/**
 * 用户查找条件
 * Created by winterchen on 2018/4/30.
 */
public class UserCond {
    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
