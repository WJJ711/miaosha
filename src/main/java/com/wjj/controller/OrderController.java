package com.wjj.controller;

import com.wjj.common.Const;
import com.wjj.error.BusinessException;
import com.wjj.error.EmBusinessError;
import com.wjj.response.CommonReturnType;
import com.wjj.service.IOrderService;
import com.wjj.service.model.OrderModel;
import com.wjj.service.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/12 22:26
 */
@Controller
@RequestMapping("/order")
@CrossOrigin(allowCredentials="true", allowedHeaders = "*")
public class OrderController {

    @Autowired
    private IOrderService iOrderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @RequestMapping(value = "/createorder",method = {RequestMethod.POST},consumes = {Const.CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam("itemId") Integer itemId,
                                        @RequestParam("amount") Integer amount,
                                        @RequestParam(value = "promoId",required = false) Integer promoId) throws BusinessException {
        //获取登录信息
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if (isLogin==null|| !isLogin.booleanValue()){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录");
        }
        //获取登录信息
        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

        OrderModel orderModel = iOrderService.createOrder(userModel.getId(), itemId,promoId,amount);
        return CommonReturnType.create(null);
    }
}
