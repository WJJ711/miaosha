package com.wjj.service.impl;

import com.wjj.dao.OrderDOMapper;
import com.wjj.dao.SequenceDOMapper;
import com.wjj.dao.UserDOMapper;
import com.wjj.dataobject.OrderDO;
import com.wjj.dataobject.SequenceDO;
import com.wjj.dataobject.UserDO;
import com.wjj.error.BusinessException;
import com.wjj.error.EmBusinessError;
import com.wjj.service.IItemService;
import com.wjj.service.IOrderService;
import com.wjj.service.IUserService;
import com.wjj.service.model.ItemModel;
import com.wjj.service.model.OrderModel;
import com.wjj.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/12 20:57
 */
@Service
public class OrderServiceImpl implements IOrderService {

    @Autowired
    private IItemService iItemService;

    @Autowired
    private IUserService iUserService;

    @Autowired
    private OrderDOMapper orderDOMapper;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Autowired
    private UserDOMapper userDOMapper;
    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId,Integer promoId,Integer amount) throws BusinessException {
        //1、校验下单状态，下单的商品是否存在，用户是否合法，购买数量是否正确
       // ItemModel itemModel = iItemService.getItemById(itemId);
        ItemModel itemModel = iItemService.getItemByIdInCache(itemId);
        if (itemModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不正确");
        }
        //UserModel userModel = iUserService.getUserById(userId);
        UserModel userModel = iUserService.getUserByIdInCache(userId);
        if (userModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户信息不正确");
        }
        if (amount<=0||amount>99){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"下单数量不正确");
        }

        //校验活动信息
        if (promoId!=null){
            //(1)校验对应活动是否存在这个使用商品
            if (promoId.intValue()!=itemModel.getPromoModel().getId()){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
                //(2)校验活动是否正在进行中
            }else if (itemModel.getPromoModel().getStatus().intValue()!=2){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动不在进行中");
            }
        }
        //2、落单减库存
        boolean reslut = iItemService.decreaseStock(itemId, amount);
        if (!reslut){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }
        //3、订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        orderModel.setPromoId(promoId);
        if (promoId!=null){
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else {
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount.toString())));
        //生成交易流水号
        orderModel.setId(generateOrderNo());

        OrderDO orderDO=convertFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);

        //商品+销量
        iItemService.increaseSales(itemId,amount);
        //4、返回前端
        return orderModel;
    }


   @Transactional(propagation = Propagation.REQUIRES_NEW )
    private String generateOrderNo(){
        StringBuilder stringBuilder = new StringBuilder();
        //订单号16位
        //前8位为时间信息，年月日
        LocalDateTime now=LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);
        //中间6位为自增序列
        Integer sequence=0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();

        sequenceDO.setCurrentValue(sequenceDO.getStep()+sequenceDO.getCurrentValue());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        String sequenceStr = String.valueOf(sequence);
        for (int i=0;i<6-sequenceStr.length();i++){
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        //最后2位为分库分表位
        stringBuilder.append("00");
        return stringBuilder.toString();
    }
    private OrderDO convertFromOrderModel(OrderModel orderModel){
        if (orderModel==null){
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel,orderDO);
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDO;
    }
}
