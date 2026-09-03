package com.botter.shop.order.config;

import cn.dev33.satoken.exception.NotLoginException;
import com.botter.shop.common.result.Result;
import com.botter.shop.common.result.ResultMsgEnum;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 处理 order 服务内的 sa-token 登录异常，统一返回业务码。
 * 正常情况下网关已经做了一级鉴权，这里是服务级兜底校验。
 */
@ControllerAdvice
@ResponseBody
public class SaTokenExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<String> handler(NotLoginException e) {
        return Result.error(ResultMsgEnum.SESSION_ERROR);
    }

    @ExceptionHandler(cn.dev33.satoken.exception.SaTokenException.class)
    public Result<String> handlerSaToken(cn.dev33.satoken.exception.SaTokenException e) {
        return Result.error(ResultMsgEnum.SESSION_ERROR);
    }
}
