package com.botter.shop.user.vo;

import com.botter.shop.user.validator.IsMobile;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-31 13:09
 * @Description 注册请求参数，password 为前端 md5 + 固定盐后的 32 位密文
 */
public class RegisterVo {

    @NotNull
    @IsMobile
    private String mobile;

    @NotNull
    @Length(min = 32)
    private String password;

    private String nickname;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        return "RegisterVo [mobile=" + mobile + ", password=" + password + ", nickname=" + nickname + "]";
    }
}
