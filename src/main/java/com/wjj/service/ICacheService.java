package com.wjj.service;


/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/18 16:55
 */
//封装本地缓存操作类
public interface ICacheService {
    //存方法
    void setCommonCache(String key, Object value);

    //取方法
    Object getFromCommonCache(String key);

}
