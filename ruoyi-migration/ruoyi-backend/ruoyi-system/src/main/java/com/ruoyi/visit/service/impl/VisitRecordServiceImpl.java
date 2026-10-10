package com.ruoyi.visit.service.impl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.visit.domain.VisitPhoto;
import com.ruoyi.visit.domain.VisitRecord;
import com.ruoyi.visit.mapper.VisitPhotoMapper;
import com.ruoyi.visit.mapper.VisitRecordMapper;
import com.ruoyi.visit.service.IStorageService;
import com.ruoyi.visit.service.IVisitRecordService;

/**
 * 走访记录 服务层实现
 *
 * 数据权限映射（四级）：
 *   管理员 admin          —— 超级管理员，若依自动跳过数据权限，看全部
 *   学校领导 school_leader —— data_scope=1（全部）
 *   学院领导 college_leader—— data_scope=2（自定，按 dept_id 过滤本学院）
 *   辅导员 counselor      —— data_scope=5（仅本人，按 user_id 过滤）
 *
 * @author ruoyi
 */
@Service
public class VisitRecordServiceImpl implements IVisitRecordService
{
    @Autowired
    private VisitRecordMapper visitRecordMapper;

    @Autowired
    private VisitPhotoMapper visitPhotoMapper;

    @Autowired
    private IStorageService storageService;

    /** 九类工作类型（与老系统 EventTypes 一致，供统计补零） */
    private static final String[] EVENT_TYPES = {
        "dorm_check", "hygiene", "talk", "class_check", "meeting",
        "visit_help", "crisis", "admin", "other"
    };

    @Override
    @DataScope(userAlias = "v", deptAlias = "v")
    public List<VisitRecord> selectVisitRecordList(VisitRecord record)
    {
        List<VisitRecord> list = visitRecordMapper.selectVisitRecordList(record);
        // 回填照片（每条记录查一次 visit_photo，列表通常 ≤ 20 条，代价可接受）
        for (VisitRecord r : list) {
            r.setPhotos(loadPhotoUrls(r.getRecordId()));
        }
        return list;
    }

    @Override
    public VisitRecord selectVisitRecordById(Long recordId)
    {
        VisitRecord r = visitRecordMapper.selectVisitRecordById(recordId);
        if (r != null) {
            r.setPhotos(loadPhotoUrls(recordId));
        }
        return r;
    }

    @Override
    @DataScope(userAlias = "v", deptAlias = "v")
    public List<VisitRecord> selectTrashList(VisitRecord record)
    {
        List<VisitRecord> list = visitRecordMapper.selectTrashList(record);
        for (VisitRecord r : list) {
            r.setPhotos(loadPhotoUrls(r.getRecordId()));
        }
        return list;
    }

    /** 按记录ID加载照片 URL（/profile/文件名） */
    private List<String> loadPhotoUrls(Long recordId)
    {
        List<String> urls = new ArrayList<>();
        if (recordId == null) {
            return urls;
        }
        for (VisitPhoto p : visitPhotoMapper.selectPhotosByRecordId(recordId)) {
            urls.add("/profile/" + p.getFilePath());
        }
        return urls;
    }

    @Override
    public List<String> insertPhotos(Long recordId, List<byte[]> images, List<String> filenames)
    {
        List<String> saved = new ArrayList<>();
        if (images == null) {
            return saved;
        }
        for (int i = 0; i < images.size(); i++) {
            String originalName = (filenames != null && i < filenames.size()) ? filenames.get(i) : null;
            String name = storageService.saveImage(images.get(i), originalName);
            VisitPhoto photo = new VisitPhoto();
            photo.setRecordId(recordId);
            photo.setFilePath(name);
            visitPhotoMapper.insertPhoto(photo);
            saved.add("/profile/" + name);
        }
        return saved;
    }

    @Override
    @DataScope(userAlias = "v", deptAlias = "v")
    public List<Map<String, Object>> monthlyStats(VisitRecord query)
    {
        // 生成截至当前月的连续 12 个月序列
        YearMonth now = YearMonth.now();
        List<String> seq = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            seq.add(now.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }
        String windowStart = seq.get(0) + "-01";

        // 用传入的 query 作为 @DataScope 注入的载体（若无则新建）
        VisitRecord q = (query != null) ? query : new VisitRecord();
        q.getParams().put("beginTime", windowStart);
        List<Map<String, Object>> rows = visitRecordMapper.selectMonthlyStats(q);

        // 按 月份 -> (类型 -> 计数) 聚合
        Map<String, Map<String, Integer>> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String month = String.valueOf(row.get("month"));
            String type = String.valueOf(row.get("event_type"));
            int cnt = ((Number) row.get("cnt")).intValue();
            counts.computeIfAbsent(month, k -> new LinkedHashMap<>())
                  .merge(type, cnt, Integer::sum);
        }

        // 补零：固定 12 行
        List<Map<String, Object>> result = new ArrayList<>();
        for (String month : seq) {
            Map<String, Integer> m = counts.getOrDefault(month, Map.of());
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("month", month);
            int total = m.values().stream().mapToInt(Integer::intValue).sum();
            out.put("total", total);
            for (String et : EVENT_TYPES) {
                out.put(et, m.getOrDefault(et, 0));
            }
            result.add(out);
        }
        return result;
    }

    @Override
    public int insertVisitRecord(VisitRecord record)
    {
        // 自动带出当前辅导员的 userId 和 deptId（供数据权限）
        record.setUserId(SecurityUtils.getUserId());
        record.setDeptId(SecurityUtils.getDeptId());
        record.setCreateBy(SecurityUtils.getUsername());
        return visitRecordMapper.insertVisitRecord(record);
    }

    @Override
    public int updateVisitRecord(VisitRecord record)
    {
        record.setUpdateBy(SecurityUtils.getUsername());
        return visitRecordMapper.updateVisitRecord(record);
    }

    @Override
    public int softDeleteVisitRecord(Long recordId)
    {
        return visitRecordMapper.softDeleteVisitRecord(recordId);
    }

    @Override
    public int restoreVisitRecord(Long recordId)
    {
        return visitRecordMapper.restoreVisitRecord(recordId);
    }

    @Override
    public int deleteVisitRecordById(Long recordId)
    {
        // 物理删除前，先收集要删的磁盘文件（照片 + 录音）
        List<String> photoFiles = new ArrayList<>();
        for (VisitPhoto p : visitPhotoMapper.selectPhotosByRecordId(recordId)) {
            photoFiles.add(p.getFilePath());
        }
        VisitRecord rec = visitRecordMapper.selectVisitRecordById(recordId);
        String audioFile = rec != null ? rec.getAudioPath() : null;

        int rows = visitRecordMapper.deleteVisitRecordById(recordId);

        // 删磁盘文件（visit_photo 表已由外键级联删除，这里补删磁盘文件）
        for (String f : photoFiles) {
            storageService.deleteImage(f);
        }
        storageService.deleteAudio(audioFile);
        return rows;
    }
}
