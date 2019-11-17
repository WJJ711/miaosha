package com.wjj.controller;

import com.wjj.error.BusinessException;
import com.wjj.error.EmBusinessError;
import com.wjj.response.CommonReturnType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/10 21:47
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Object handlerException(HttpServletRequest request, Exception ex){
        ex.printStackTrace();
        Map<String,Object> responseData=new HashMap<>();
        if (ex instanceof BusinessException){
            BusinessException businessException = (BusinessException) ex;
            responseData.put("errCode",businessException.getErrCode());
            responseData.put("errMsg",businessException.getErrMsg());
        }else if(ex instanceof ServletRequestBindingException){
            responseData.put("errCode",EmBusinessError.UNKOWN_ERROR.getErrCode());
            responseData.put("errMsg","url绑定路由问题");
        }else if (ex instanceof NoHandlerFoundException){
            responseData.put("errCode",EmBusinessError.UNKOWN_ERROR.getErrCode());
            responseData.put("errMsg","没有找到对应的访问路径");
        }else {
            responseData.put("errCode", EmBusinessError.UNKOWN_ERROR.getErrCode());
            responseData.put("errMsg",EmBusinessError.UNKOWN_ERROR.getErrMsg());
        }

        return CommonReturnType.create(responseData,"fail");
    }
}
