package com.ruoyi.visit.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.visit.domain.VisitRecord;

/**
 * 走访记录 服务层
 *
 * @author ruoyi
 */
public interface IVisitRecordService
{
    /** 条件分页查询（含四级数据权限） */
    List<VisitRecord> selectVisitRecordList(VisitRecord record);

    /** 回收站查询（软删除记录，含四级数据权限） */
    List<VisitRecord> selectTrashList(VisitRecord record);

    /** 按主键查询 */
    VisitRecord selectVisitRecordById(Long recordId);

    /** 新增照片（关联记录，返回文件名列表，URL 为 /profile/文件名） */
    List<String> insertPhotos(Long recordId, java.util.List<byte[]> images, java.util.List<String> filenames);

    /** 月度统计（固定 12 行，query 为 @DataScope 注入载体） */
    List<Map<String, Object>> monthlyStats(VisitRecord query);

    /** 新增（AI 识别结果 + 补录日期） */
    int insertVisitRecord(VisitRecord record);

    /** 修改 */
    int updateVisitRecord(VisitRecord record);

    /** 软删除 */
    int softDeleteVisitRecord(Long recordId);

    /** 恢复 */
    int restoreVisitRecord(Long recordId);

    /** 彻底删除 */
    int deleteVisitRecordById(Long recordId);
}
