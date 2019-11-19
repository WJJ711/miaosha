package com.wjj.service;

import com.wjj.error.BusinessException;
import com.wjj.service.model.ItemModel;

import java.util.List;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/11 21:08
 */
public interface IItemService {
    //创建商品
    ItemModel createItem(ItemModel itemModel) throws BusinessException;
    //商品列表浏览
    List<ItemModel> listItem();
    //商品详情浏览
    ItemModel getItemById(Integer id);

    boolean decreaseStock(Integer itemId,Integer amount) throws BusinessException;

    //商品销量增加
    void increaseSales(Integer itemId,Integer amount) throws BusinessException;

    ItemModel getItemByIdInCache(Integer id);

}
