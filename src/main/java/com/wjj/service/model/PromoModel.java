package com.wjj.service.model;

import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;

import java.math.BigDecimal;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/12 23:13
 */
@Getter
@Setter
public class PromoModel {
    private Integer id;

    //秒杀活动状态1表示还为开始，2表示进行中，3表示已经结束,0表示没秒杀
    private Integer status;
    //秒杀活动名称
    private String promoName;

    //秒杀活动的开始时间
    private DateTime startDate;

    private DateTime endDate;
    //秒杀活动的适用商品
    private Integer itemId;

    private BigDecimal promoItemPrice;
}
