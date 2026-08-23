package com.botter.shop.common.result;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 18:47
 * @Description 描述信息
 */
public class Result<T> {
    private int code;
    private String msg;
    private T data;


    public static <T> Result<T> success(T data)    {return new Result<T>(data);}
    public static <T> Result<T> error(CodeMsg msg) {return new Result<T>(msg);}
    public static <T> Result<T> error(ResultMsgEnum msg) {return new Result<T>(msg);}

    public Result() {

    }

    private Result(T data) {
        this.code = 200;
        this.data = data;
        this.msg = "success";
    }

    private Result(int code, String message) {
        this.code = code;
        this.msg = message;
    }

    private Result(CodeMsg codeMsg) {
        if (codeMsg == null) {
            return;
        }
        this.code = codeMsg.getCode();
        this.msg = codeMsg.getMsg();
    }

    private  Result(ResultMsgEnum msg) {
        if (msg == null) {
            return;
        }
        this.code = msg.getCode();
        this.msg = msg.getMsg();
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

}
