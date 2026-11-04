package com.ruoyi.visit.mapper;

import java.util.List;
import com.ruoyi.visit.domain.VisitItem;

/**
 * 走访事项 Mapper
 */
public interface VisitItemMapper
{
    List<VisitItem> selectVisitItemList(VisitItem item);

    VisitItem selectVisitItemById(Long itemId);

    int insertVisitItem(VisitItem item);

    int updateVisitItem(VisitItem item);

    int softDeleteVisitItem(Long itemId);

    /** 取某年份的最大序号（用于生成 XS-年份-序号 编号） */
    Integer selectMaxSeqOfYear(String yearPrefix);

    /** 事项统计：各状态数量 + 超期数（未结案且已过截止） */
    List<java.util.Map<String, Object>> selectItemStats(VisitItem item);
}
