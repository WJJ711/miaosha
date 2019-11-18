package com.wjj.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wjj.service.ICacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/18 16:57
 */
@Service
public class CacheServiceImpl implements ICacheService {

    private Cache<String,Object> commonCache=null;

    @PostConstruct
    public void init(){
        commonCache= CacheBuilder.newBuilder()
                //设置缓存容器的初始容量为10
        .initialCapacity(10)
                //设置缓存中最大可以存储100个KEY，超过100个之后会按照LRU的策略移除缓存项
        .maximumSize(100)
                //设置缓存失效时间
        .expireAfterWrite(60, TimeUnit.SECONDS).build();
    }
    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key,value);
    }

    @Override
    public Object getFromCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}
