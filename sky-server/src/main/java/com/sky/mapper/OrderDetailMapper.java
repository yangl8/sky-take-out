package com.sky.mapper;

import java.util.List;
import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import com.sky.entity.OrderDetail;
import com.sky.vo.ItemSalesVO;

@Mapper 
public interface OrderDetailMapper {

    void insertBatch(List<OrderDetail> orderDetailList);

    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);

    List<OrderDetail> getByOrderIds(@Param("orderIds") List<Long> orderIds);

    List<ItemSalesVO> getDishMonthlySales(@Param("ids") List<Long> ids,
                                          @Param("begin") LocalDateTime begin,
                                          @Param("end") LocalDateTime end);

    List<ItemSalesVO> getSetmealMonthlySales(@Param("ids") List<Long> ids,
                                             @Param("begin") LocalDateTime begin,
                                             @Param("end") LocalDateTime end);

}
