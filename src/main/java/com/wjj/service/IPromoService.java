package com.wjj.service;

import com.wjj.service.model.PromoModel;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/12 23:21
 */
public interface IPromoService {
    PromoModel getPromoByItemId(Integer itemId);
}
