package com.wjj.service.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/12 20:33
 */
@Getter
@Setter
public class OrderModel {
    //201810219999001
    private String id;

    private Integer userId;

    private Integer itemId;

    //若非空，则表示以秒杀商品方式下单
    private Integer promoId;

    //若promoId非空，则表示为秒杀价格
    private BigDecimal itemPrice;
    //购买数量
    private Integer amount;
    //购买金额
    //若promoId非空，则表示为秒杀价格
    private BigDecimal orderPrice;
}
