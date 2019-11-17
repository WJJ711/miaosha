package com.wjj.error;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/10 20:42
 */
public interface CommonError {
    int getErrCode();
    String getErrMsg();
    CommonError setErrMsg(String errMsg);
}
