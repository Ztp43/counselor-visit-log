package com.ruoyi.visit.mapper;

import java.util.List;
import com.ruoyi.visit.domain.VisitAccessLog;

/**
 * 敏感记录访问审计 Mapper
 */
public interface VisitAccessLogMapper
{
    int insertAccessLog(VisitAccessLog log);

    List<VisitAccessLog> selectAccessLogs(Long recordId);

    /** 审计日志列表（分页，issue #37） */
    List<VisitAccessLog> selectAccessLogPage(VisitAccessLog query);
}
