package com.wjj.service.impl;

import com.wjj.dao.PromoDOMapper;
import com.wjj.dataobject.PromoDO;
import com.wjj.error.BusinessException;
import com.wjj.error.EmBusinessError;
import com.wjj.service.IItemService;
import com.wjj.service.IPromoService;
import com.wjj.service.IUserService;
import com.wjj.service.model.ItemModel;
import com.wjj.service.model.PromoModel;
import com.wjj.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/12 23:22
 */
@Service
public class PromoServiceImpl implements IPromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private IItemService iItemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IUserService iUserService;



    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        PromoModel promoModel = convertFromDataObject(promoDO);
        if (promoModel==null){
            return null;
        }
        //判断当前时间是否秒杀活动即将开始或正在进行
        if (promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else {
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO==null||promoDO.getItemId()==null||promoDO.getItemId().intValue()==0){
            return;
        }
        ItemModel itemModel = iItemService.getItemById(promoDO.getItemId());
        //将库存同步到redis内
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(),itemModel.getStock());

        //将大闸的限制数字设到redis内
        redisTemplate.opsForValue().set("promo_door_count_"+promoId,itemModel.getStock().intValue()*5);
    }

    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) {

        //判断是否库存已售罄，若对应的售罄key存在，则直接返回下单失败
        if (redisTemplate.hasKey("promo_item_stock_invalid_"+itemId)){
            return null;
        }


        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        PromoModel promoModel = convertFromDataObject(promoDO);
        //判断当前时间是否秒杀活动即将开始或正在进行
        if (promoModel==null){
            return null;
        }
        if (promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else {
            promoModel.setStatus(2);
        }

        //判断活动是否正在进行
        if (promoModel.getStatus().intValue()!=2){
            return null;
        }
        //校验下单状态，下单的商品是否存在
        // ItemModel itemModel = iItemService.getItemById(itemId);
        ItemModel itemModel = iItemService.getItemByIdInCache(itemId);
        if (itemModel==null){
            return null;
        }
        //判断用户信息是否存在
        //UserModel userModel = iUserService.getUserById(userId);
        UserModel userModel = iUserService.getUserByIdInCache(userId);
        if (userModel==null){
            return null;
        }

        Long result = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if (result.longValue()<0){
            return null;
        }
        //生成token，并且存入redis内给一个5min的有效期
        String token= UUID.randomUUID().toString().replace("-","");
        redisTemplate.opsForValue().set("promo_token_"+promoId+"_userid_"+userId+"_itemid_"+itemId,token);
        redisTemplate.expire("promo_token_"+promoId+"_userid_"+userId+"_itemid_"+itemId,5, TimeUnit.MINUTES);
        return token;
    }

    private PromoModel convertFromDataObject(PromoDO promoDO){
        if (promoDO==null){
            return null;
        }
        PromoModel promoModel=new PromoModel();
        BeanUtils.copyProperties(promoDO,promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }
}
