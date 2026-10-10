package com.ruoyi.visit.mapper;

import java.util.List;
import com.ruoyi.visit.domain.VisitPhoto;

/**
 * 走访记录照片 数据层
 *
 * @author ruoyi
 */
public interface VisitPhotoMapper
{
    /** 按记录ID查询照片列表 */
    List<VisitPhoto> selectPhotosByRecordId(Long recordId);

    /** 批量插入照片 */
    int insertPhoto(VisitPhoto photo);
}
