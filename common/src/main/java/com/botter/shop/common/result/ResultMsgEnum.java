package com.botter.shop.common.result;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 18:53
 * @Description 描述信息
 */
public enum ResultMsgEnum {
    // ==================== 成功 ====================
    SUCCESS(200, "success"),

    // ==================== 服务端/通用异常 5001xx ====================
    SERVER_ERROR(500100, "服务端错误异常"),
    BIND_ERROR(500101, "参数校验异常:%s"),
    SESSION_ERROR(500102, "Session不存在或已失效"),
    REQUEST_METHOD_ERROR(500103, "请求方式不支持"),
    MEDIA_TYPE_ERROR(500104, "请求数据格式错误"),
    FREQUENCY_LIMIT(500105, "请求过于频繁，请稍后再试"),
    OPERATION_FAIL(500106, "操作失败"),
    DATA_NOT_EXIST(500107, "数据不存在"),
    DATA_DUPLICATE(500108, "数据已存在，请勿重复操作"),
    TOKEN_EMPTY(500109, "Token不能为空"),

    // ==================== 商品模块异常 5002xx ====================
    GOODS_ADD_BRAND_ERROR(500200, "商品添加失败"),
    GOODS_NOT_EXIST(500201, "商品不存在"),
    GOODS_OFF_SHELF(500202, "商品已下架"),
    GOODS_STOCK_SHORTAGE(500203, "商品库存不足"),
    GOODS_UPDATE_ERROR(500204, "商品更新失败"),
    GOODS_DELETE_ERROR(500205, "商品删除失败"),
    CATEGORY_NOT_EXIST(500206, "商品分类不存在"),

    // ==================== 用户/认证模块异常 5003xx ====================
    MOBILE_NOT_EXIST(500300, "手机号未注册"),
    PASSWORD_ERROR(500301, "密码错误"),
    JWT_PARSE_ERROR(500302, "token解析失败"),
    JWT_EXPIRED(500303, "token已过期，请重新登录"),
    JWT_SIGN_ERROR(500304, "token签名非法"),
    MOBILE_FORMAT_ERROR(500305, "手机号格式不正确"),
    USER_NOT_EXIST(500306, "用户不存在"),
    USER_DISABLED(500307, "账号已被禁用"),
    USER_REGISTER_FAIL(500308, "用户注册失败"),
    CODE_ERROR(500309, "验证码错误"),
    CODE_EXPIRED(500310, "验证码已过期"),

    // ==================== 订单模块异常 5004xx ====================
    ORDER_CREATE_FAIL(500400, "创建订单失败"),
    ORDER_NOT_EXIST(500401, "订单不存在"),
    ORDER_STATUS_ERROR(500402, "订单状态异常"),
    ORDER_PAY_TIMEOUT(500403, "订单支付超时"),
    ORDER_CANCEL_FAIL(500404, "订单取消失败"),
    ORDER_DELIVER_FAIL(500405, "订单发货失败"),

    // ==================== 购物车模块异常 5005xx ====================
    CART_ADD_FAIL(500500, "加入购物车失败"),
    CART_ITEM_NOT_EXIST(500501, "购物车条目不存在"),
    CART_CLEAR_FAIL(500502, "清空购物车失败"),

    // ==================== 支付模块异常 5006xx ====================
    PAY_CREATE_FAIL(500600, "生成支付单失败"),
    PAY_NOTIFY_VERIFY_FAIL(500601, "支付回调验签失败"),
    PAY_STATUS_ERROR(500602, "支付状态异常"),
    REFUND_FAIL(500603, "退款申请失败"),

    // ==================== 权限模块异常 5007xx ====================
    NO_PERMISSION(500700, "没有操作权限"),
    ROLE_NOT_EXIST(500701, "角色不存在"),


    ES_SERVICE_ERROR(5008001,"ES错误,错误信息:%s");


    private int code;
    private String msg;

    private ResultMsgEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public CodeMsg fillArgs(Object... args) {
        return new CodeMsg(this.code, String.format(this.msg, args));
    }

    public static CodeMsg fillArgs(ResultMsgEnum errorEnum, Object... args) {
        if (errorEnum == null) {
            return new CodeMsg(999, "未知错误");
        }
        return new CodeMsg(errorEnum.code, String.format(errorEnum.msg, args));
    }
    public String getMsgByCode(int code){
        for (ResultMsgEnum resultMsgEnum : ResultMsgEnum.values()) {
            if (resultMsgEnum.getCode()==code){
                return resultMsgEnum.getMsg();
            }
        }
        return null;
    }

    public ResultMsgEnum getEnumByCode(int code){
        for (ResultMsgEnum resultMsgEnum : ResultMsgEnum.values()) {
            if (resultMsgEnum.getCode()==code){
                return resultMsgEnum;
            }
        }
        return null;
    }

    public int getCode() {
        return code;
    }
    public String getMsg() {
        return msg;
    }
}
