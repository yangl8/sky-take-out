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

    /**
     * 批量插入订单明细数据
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);

    /**
     * 根据订单id查询订单明细
     * @param orderId
     * @return
     */
    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);

    /**
     * 根据订单id集合批量查询订单明细
     *
     * @param orderIds 当前页订单id集合
     * @return 按订单id及明细id排序的订单明细集合
     */
    List<OrderDetail> getByOrderIds(@Param("orderIds") List<Long> orderIds);

    /**
     * 查询指定菜品的本月销量
     * @param ids 菜品id集合
     * @param begin 本月开始时间
     * @param end 下月开始时间
     * @return 菜品销量集合
     */
    List<ItemSalesVO> getDishMonthlySales(@Param("ids") List<Long> ids,
                                          @Param("begin") LocalDateTime begin,
                                          @Param("end") LocalDateTime end);

    /**
     * 查询指定套餐的本月销量
     * @param ids 套餐id集合
     * @param begin 本月开始时间
     * @param end 下月开始时间
     * @return 套餐销量集合
     */
    List<ItemSalesVO> getSetmealMonthlySales(@Param("ids") List<Long> ids,
                                             @Param("begin") LocalDateTime begin,
                                             @Param("end") LocalDateTime end);

}
