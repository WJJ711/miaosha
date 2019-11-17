package com.wjj.controller;

import com.wjj.common.Const;
import com.wjj.controller.viewobject.UserVO;
import com.wjj.error.BusinessException;
import com.wjj.error.EmBusinessError;
import com.wjj.response.CommonReturnType;
import com.wjj.service.IUserService;
import com.wjj.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/10 16:18
 */
@Controller
@RequestMapping("/user")
@CrossOrigin(allowCredentials="true", allowedHeaders = "*")
public class UserController {

    @Autowired
    private IUserService iUserService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    //用户登录接口
    @RequestMapping(value = "/login",method = {RequestMethod.POST},consumes = {Const.CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam("telphone") String telphone,
                                  @RequestParam("password") String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //入参校验
        if (StringUtils.isBlank(telphone)||StringUtils.isBlank(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户名或密码为空");
        }
        //用户登录服务，用来校验用户登录是否合法
        UserModel userModel = iUserService.validateLogin(telphone, this.EncodeByMd5(password));
        //将登录凭证加入到用户登录成功的session内
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);
        return CommonReturnType.create(null);
    }
    //用户注册接口
    @RequestMapping(value = "/register",method = {RequestMethod.POST},consumes = {Const.CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam("telphone") String telphone,
                                     @RequestParam("otpCode") String otpCode,
                                     @RequestParam("name") String name,
                                     @RequestParam("gender") Integer gender,
                                     @RequestParam("age") Integer age,
                                     @RequestParam("password") String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //验证手机号和对应的otpcode相符合
        String inSessionOtpCode = (String) this.httpServletRequest.getSession().getAttribute(telphone);
        if (!StringUtils.equals(inSessionOtpCode,otpCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"短信验证码不符合");
        }
        //用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setEncrptPassword(this.EncodeByMd5(password));
        userModel.setTelphone(telphone);
        userModel.setRegisterMode("byphone");
        userModel.setName(name);
        userModel.setGender(Byte.valueOf(gender.toString()));
        userModel.setAge(age);
        iUserService.register(userModel);
        return CommonReturnType.create(null);
    }
    public String EncodeByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //确定计算方法
        MessageDigest md5=MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder = new BASE64Encoder();
        String newStr = base64Encoder.encode(md5.digest(str.getBytes("utf-8")));
        return newStr;
    }
    @RequestMapping(value = "/getotp",method = {RequestMethod.POST},consumes = {Const.CONTENT_TYPE_FORMED})
    @ResponseBody
    //otp全称是one-time-password,也称动态口令,
    public CommonReturnType getOtp(@RequestParam("telphone")String telphone){
        //需要按照一定的规则生成OTP验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt+=10000;
        String otpCode=String.valueOf(randomInt);
        //将OTP验证码同对应用户的手机号关联,暂时用HttpSession
        httpServletRequest.getSession().setAttribute(telphone,otpCode);
        //将OTP验证码通过短信通道发送给用户,省略
        System.out.println("telPhone="+telphone+"&otpCode="+otpCode);
        return CommonReturnType.create(null) ;
    }

    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(value = "id") Integer id) throws BusinessException {
        //调用service服务获取对应id的用户对象返回给前端
        UserModel userModel = iUserService.getUserById(id);
        if (userModel==null){
            //throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
            userModel.setEncrptPassword("1233");
        }
        UserVO userVO = convertFromModel(userModel);
        return CommonReturnType.create(userVO);
    }
    private UserVO convertFromModel(UserModel userModel){
        if (userModel==null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }

}
