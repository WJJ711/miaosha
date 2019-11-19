package com.wjj.service;

import com.wjj.error.BusinessException;
import com.wjj.service.model.UserModel;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/10 16:27
 */
public interface IUserService {
    UserModel getUserById(Integer id);
    void register(UserModel userModel) throws BusinessException;
    /*
    这里encrptPassword已经是加密后的密码
     */
    UserModel validateLogin(String telphone,String encrptPassword) throws BusinessException;

    UserModel getUserByIdInCache(Integer id);
}
