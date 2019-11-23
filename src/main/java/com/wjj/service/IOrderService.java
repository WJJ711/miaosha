package com.wjj.service;

import com.wjj.error.BusinessException;
import com.wjj.service.model.OrderModel;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/12 20:51
 */
public interface IOrderService {
    OrderModel createOrder(Integer userId,Integer itemId,Integer promoId,Integer amount,String stockLogId) throws BusinessException;
}
