package com.ruoyi.visit.mapper;

import java.util.List;
import com.ruoyi.visit.domain.VisitItemLog;

/**
 * 事项跟进时间线 Mapper
 */
public interface VisitItemLogMapper
{
    List<VisitItemLog> selectLogsByItemId(Long itemId);

    int insertLog(VisitItemLog log);
}
