package com.ruoyi.visit.service.impl;

import java.time.Year;
import java.util.List;
import org.springframework.stereotype.Service;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.visit.domain.VisitItem;
import com.ruoyi.visit.mapper.VisitItemMapper;
import com.ruoyi.visit.service.IVisitItemService;

/**
 * 走访事项服务（issue #11）
 *
 * 复用与走访记录相同的四级数据权限（@DataScope + userAlias/deptAlias = i）。
 * 编号生成：XS-年份-三位序号（如 XS-2026-001），跨年自动归零重新编号。
 */
@Service
public class VisitItemServiceImpl implements IVisitItemService
{
    private final VisitItemMapper visitItemMapper;

    public VisitItemServiceImpl(VisitItemMapper visitItemMapper)
    {
        this.visitItemMapper = visitItemMapper;
    }

    @Override
    @DataScope(userAlias = "i", deptAlias = "i")
    public List<VisitItem> selectVisitItemList(VisitItem item)
    {
        return visitItemMapper.selectVisitItemList(item);
    }

    @Override
    public VisitItem selectVisitItemById(Long itemId)
    {
        return visitItemMapper.selectVisitItemById(itemId);
    }

    @Override
    public int insertVisitItem(VisitItem item)
    {
        // 补全：创建人、所属学院、默认状态、编号
        Long userId = SecurityUtils.getUserId();
        item.setUserId(userId);
        item.setDeptId(SecurityUtils.getDeptId());
        if (item.getStatus() == null || item.getStatus().isBlank())
        {
            item.setStatus("pending");
        }
        item.setItemNo(generateItemNo());
        return visitItemMapper.insertVisitItem(item);
    }

    @Override
    public int updateVisitItem(VisitItem item)
    {
        return visitItemMapper.updateVisitItem(item);
    }

    @Override
    public int deleteVisitItemById(Long itemId)
    {
        return visitItemMapper.softDeleteVisitItem(itemId);
    }

    @Override
    public String generateItemNo()
    {
        String prefix = "XS-" + Year.now().getValue() + "-";
        Integer maxSeq = visitItemMapper.selectMaxSeqOfYear(prefix);
        int seq = (maxSeq == null ? 0 : maxSeq) + 1;
        return prefix + String.format("%03d", seq);
    }
}
