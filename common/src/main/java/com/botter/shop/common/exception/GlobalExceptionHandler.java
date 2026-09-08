package com.botter.shop.common.exception;

import com.botter.shop.common.result.CodeMsg;
import com.botter.shop.common.result.Result;
import com.botter.shop.common.result.ResultMsgEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 19:18
 * @Description 描述信息
 */

@ControllerAdvice(basePackages = "com.botter.shop")
@ResponseBody
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = Exception.class)
    public Result<String> exceptionHandler(HttpServletRequest request, Exception e) {
        // 业务异常是预期内的"拒绝"（已抢过/售罄/未开始等），压测时每个被拒请求都会走到这。
        // 千万别在这里打 error+堆栈：控制台输出是全局同步锁，会把整个服务的吞吐摁死。
        if (e instanceof GlobalException) {
            GlobalException ex = (GlobalException) e;
            logger.debug("业务拒绝 {}: {}", request.getRequestURI(), ex.getCm().getMsg());
            return Result.error(ex.getCm());
        }
        // 意外异常才值得打完整堆栈
        logger.error("exceptionHandler error: ", e);
        if (e instanceof BindException) {
            BindException ex = (BindException) e;
            List<ObjectError> errors = ex.getAllErrors();
            ObjectError error = errors.get(0);
            String msg = error.getDefaultMessage();
            return Result.error(ResultMsgEnum.BIND_ERROR.fillArgs(msg));
        } else {
            return Result.error(ResultMsgEnum.SERVER_ERROR);
        }
    }

}
