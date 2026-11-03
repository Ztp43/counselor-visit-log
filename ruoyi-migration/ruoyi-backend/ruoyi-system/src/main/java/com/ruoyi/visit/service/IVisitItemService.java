package com.ruoyi.visit.service;

import java.util.List;
import com.ruoyi.visit.domain.VisitItem;

/**
 * 走访事项服务接口
 */
public interface IVisitItemService
{
    List<VisitItem> selectVisitItemList(VisitItem item);

    VisitItem selectVisitItemById(Long itemId);

    int insertVisitItem(VisitItem item);

    int updateVisitItem(VisitItem item);

    int deleteVisitItemById(Long itemId);

    /** 生成事项编号 XS-年份-序号 */
    String generateItemNo();

    /** 事项跟进时间线 */
    List<com.ruoyi.visit.domain.VisitItemLog> selectItemLogs(Long itemId);

    /** 手动新增跟进记录（action=follow） */
    int addFollowLog(Long itemId, String content);
}
