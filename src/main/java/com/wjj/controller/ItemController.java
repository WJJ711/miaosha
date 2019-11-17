package com.wjj.controller;

import com.wjj.common.Const;
import com.wjj.controller.viewobject.ItemVO;
import com.wjj.error.BusinessException;
import com.wjj.response.CommonReturnType;
import com.wjj.service.IItemService;
import com.wjj.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wjj
 * @version 1.0
 * @date 2019/11/11 21:58
 */
@Controller
@RequestMapping("/item")
@CrossOrigin(allowCredentials="true", allowedHeaders = "*")
public class ItemController {
    @Autowired
    private IItemService iItemService;

    @RequestMapping(value = "/create",method = {RequestMethod.POST},consumes = {Const.CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam("title") String title,
                                       @RequestParam("description") String description,
                                       @RequestParam("price") BigDecimal price,
                                       @RequestParam("stock") Integer stock,
                                       @RequestParam("imgUrl") String imgUrl) throws BusinessException {
        ItemModel itemModel = new ItemModel();
        itemModel.setStock(stock);
        itemModel.setDescription(description);
        itemModel.setPrice(price);
        itemModel.setImgUrl(imgUrl);
        itemModel.setTitle(title);
        ItemModel itemModelForReturn = iItemService.createItem(itemModel);
        ItemVO itemVO=convertVOFromModel(itemModelForReturn);
        return CommonReturnType.create(itemVO);
    }

    @RequestMapping(value = "/get",method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getItem(@RequestParam("id") Integer id){
        ItemModel itemModel = iItemService.getItemById(id);
        ItemVO itemVO = convertVOFromModel(itemModel);

        return CommonReturnType.create(itemVO);
    }
    @RequestMapping(value = "/list",method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getList(){
        List<ItemModel> itemModelList = iItemService.listItem();
        List<ItemVO> itemVOList = itemModelList.stream().map(itemModel -> {
            return this.convertVOFromModel(itemModel);
        }).collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);

    }

    private ItemVO convertVOFromModel(ItemModel itemModel){
        if (itemModel==null){
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel,itemVO);
        if(itemModel.getPromoModel()!=null){
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else {
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }
}
