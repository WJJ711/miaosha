package com.wjj.validator;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/11 18:27
 */
@Getter
@Setter
public class ValidationResult {
    //判断校验结果是否有错
    private boolean hasErrors=false;

    //存放错误信息的map
    private Map<String,String> errMsgMap=new HashMap<>();

    //实现通过格式化字符串信息获取错误结果的msg方法
    public String getErrMsg(){
        return StringUtils.join(errMsgMap.values().toArray(),",");
    }
}
