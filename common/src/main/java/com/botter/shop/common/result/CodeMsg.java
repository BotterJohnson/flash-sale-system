package com.botter.shop.common.result;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 18:50
 * @Description 描述信息
 */
public class CodeMsg {
    private int code;
    private String msg;


    private CodeMsg(){}

    public CodeMsg(ResultMsgEnum resultMsgEnum){
        this.code = resultMsgEnum.getCode();
        this.msg = resultMsgEnum.getMsg();
    }

    CodeMsg(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public CodeMsg fillArgs(Object... args) {
        int code = this.code;
        String message = String.format(this.msg, args);
        return new CodeMsg(code, message);
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }


    @Override
    public String toString() {
        return "CodeMsg{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}