package com.wjj.dao;

import com.wjj.dataobject.ItemDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface ItemDOMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ItemDO record);

    int insertSelective(ItemDO record);

    ItemDO selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ItemDO record);

    int updateByPrimaryKey(ItemDO record);

    List<ItemDO> listItem();

    int increaseSales(@Param("amount") int amount, @Param("id") int id);
}