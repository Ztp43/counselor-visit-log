package com.ruoyi.visit.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.visit.domain.VisitRecord;

/**
 * 走访记录 数据层
 *
 * @author ruoyi
 */
public interface VisitRecordMapper
{
    /** 条件分页查询（含数据权限过滤，@DataScope 注入 params.dataScope） */
    List<VisitRecord> selectVisitRecordList(VisitRecord record);

    /** 回收站查询（deleted_at is not null，含数据权限过滤） */
    List<VisitRecord> selectTrashList(VisitRecord record);

    /** 按主键查询 */
    VisitRecord selectVisitRecordById(Long recordId);

    /** 月度统计（按 record_date 或 create_time 分组，各类型计数） */
    List<Map<String, Object>> selectMonthlyStats(VisitRecord record);

    /** 新增 */
    int insertVisitRecord(VisitRecord record);

    /** 修改 */
    int updateVisitRecord(VisitRecord record);

    /** 软删除 */
    int softDeleteVisitRecord(Long recordId);

    /** 恢复 */
    int restoreVisitRecord(Long recordId);

    /** 物理删除（彻底删除） */
    int deleteVisitRecordById(Long recordId);
}
