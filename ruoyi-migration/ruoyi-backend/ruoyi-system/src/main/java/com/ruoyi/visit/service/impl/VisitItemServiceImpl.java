package com.ruoyi.visit.service.impl;

import java.time.Year;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.visit.domain.VisitItem;
import com.ruoyi.visit.domain.VisitItemLog;
import com.ruoyi.visit.mapper.VisitItemLogMapper;
import com.ruoyi.visit.mapper.VisitItemMapper;
import com.ruoyi.visit.service.IVisitItemService;

/**
 * 走访事项服务（issue #11）
 *
 * 复用与走访记录相同的四级数据权限（@DataScope + userAlias/deptAlias = i）。
 * 编号生成：XS-年份-三位序号（如 XS-2026-001），跨年自动归零重新编号。
 * 跟进时间线：创建/状态变更自动写一条时间线，另支持手动新增跟进记录。
 */
@Service
public class VisitItemServiceImpl implements IVisitItemService
{
    private final VisitItemMapper visitItemMapper;
    private final VisitItemLogMapper visitItemLogMapper;

    public VisitItemServiceImpl(VisitItemMapper visitItemMapper, VisitItemLogMapper visitItemLogMapper)
    {
        this.visitItemMapper = visitItemMapper;
        this.visitItemLogMapper = visitItemLogMapper;
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
    @Transactional
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
        int rows = visitItemMapper.insertVisitItem(item);

        // 自动写时间线：创建
        writeLog(item.getItemId(), "create", "创建事项「" + item.getTitle() + "」", null, item.getStatus());
        return rows;
    }

    @Override
    @Transactional
    public int updateVisitItem(VisitItem item)
    {
        // 状态变更时自动写时间线（先取旧状态）
        VisitItem old = visitItemMapper.selectVisitItemById(item.getItemId());
        String oldStatus = old != null ? old.getStatus() : null;
        int rows = visitItemMapper.updateVisitItem(item);

        if (item.getStatus() != null && !item.getStatus().equals(oldStatus))
        {
            writeLog(item.getItemId(), "status", statusChangeText(oldStatus, item.getStatus()),
                    oldStatus, item.getStatus());
        }
        return rows;
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

    @Override
    public List<VisitItemLog> selectItemLogs(Long itemId)
    {
        return visitItemLogMapper.selectLogsByItemId(itemId);
    }

    @Override
    public int addFollowLog(Long itemId, String content)
    {
        VisitItemLog log = new VisitItemLog();
        log.setItemId(itemId);
        log.setAction("follow");
        log.setContent(content);
        log.setCreateBy(SecurityUtils.getUsername());
        return visitItemLogMapper.insertLog(log);
    }

    /** 写一条时间线（自动） */
    private void writeLog(Long itemId, String action, String content, String from, String to)
    {
        VisitItemLog log = new VisitItemLog();
        log.setItemId(itemId);
        log.setAction(action);
        log.setContent(content);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setCreateBy(SecurityUtils.getUsername());
        visitItemLogMapper.insertLog(log);
    }

    private String statusChangeText(String from, String to)
    {
        String f = statusCn(from);
        String t = statusCn(to);
        return "状态由「" + f + "」变更为「" + t + "」";
    }

    private String statusCn(String s)
    {
        if (s == null) return "无";
        return switch (s) {
            case "pending" -> "待处理";
            case "following" -> "跟进中";
            case "closed" -> "已结案";
            default -> s;
        };
    }
}
