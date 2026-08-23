package com.botter.shop.common.exception;

import com.botter.shop.common.result.CodeMsg;
import com.botter.shop.common.result.ResultMsgEnum;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 19:16
 * @Description 描述信息
 */
public class GlobalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private CodeMsg cm;

    public GlobalException(CodeMsg cm) {
        super(cm.toString());
        this.cm = cm;
    }

    public GlobalException(ResultMsgEnum resultMsgEnum) {
        super(resultMsgEnum.getMsg());
        this.cm = new CodeMsg(resultMsgEnum);
    }


    public CodeMsg getCm() {
        return cm;
    }
}
