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
}
