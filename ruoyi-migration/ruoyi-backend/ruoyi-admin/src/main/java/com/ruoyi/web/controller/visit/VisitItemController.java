package com.ruoyi.web.controller.visit;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.visit.domain.VisitItem;
import com.ruoyi.visit.domain.VisitRecord;
import com.ruoyi.visit.service.IVisitItemService;
import com.ruoyi.visit.service.IVisitOrgService;
import com.ruoyi.visit.service.IVisitRecordService;

/**
 * 走访事项台账（issue #11 事项/跟进闭环体系）
 */
@RestController
@RequestMapping("/visit/item")
public class VisitItemController extends BaseController
{
    private final IVisitItemService visitItemService;
    private final IVisitOrgService visitOrgService;
    private final IVisitRecordService visitRecordService;

    public VisitItemController(IVisitItemService visitItemService, IVisitOrgService visitOrgService,
                               IVisitRecordService visitRecordService)
    {
        this.visitItemService = visitItemService;
        this.visitOrgService = visitOrgService;
        this.visitRecordService = visitRecordService;
    }

    /** 事项列表（分页，四级数据权限） */
    @PreAuthorize("@ss.hasPermi('visit:item:list')")
    @GetMapping("/list")
    public TableDataInfo list(VisitItem item)
    {
        startPage();
        List<VisitItem> list = visitItemService.selectVisitItemList(item);
        return getDataTable(list);
    }

    /** 事项详情 */
    @PreAuthorize("@ss.hasPermi('visit:item:query')")
    @GetMapping("/{itemId}")
    public AjaxResult getInfo(@PathVariable Long itemId)
    {
        return success(visitItemService.selectVisitItemById(itemId));
    }

    /** 事项关联的工作日志（issue #34，受 @DataScope 数据隔离） */
    @PreAuthorize("@ss.hasPermi('visit:item:query')")
    @GetMapping("/{itemId}/records")
    public AjaxResult records(@PathVariable Long itemId)
    {
        VisitRecord query = new VisitRecord();
        query.setItemId(itemId);
        return success(visitRecordService.selectVisitRecordList(query));
    }

    /** 新增事项 */
    @PreAuthorize("@ss.hasPermi('visit:item:add')")
    @Log(title = "走访事项", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VisitItem item)
    {
        if (item.getTitle() == null || item.getTitle().isBlank())
        {
            return error("事项标题不能为空");
        }
        if (item.getCategory() == null || item.getCategory().isBlank())
        {
            return error("请选择问题类别");
        }
        visitItemService.insertVisitItem(item);
        // 返回完整实体（含回填的 itemId 和生成的 itemNo），供前端关联日志使用
        return success(item);
    }

    /** 编辑事项（含状态流转） */
    @PreAuthorize("@ss.hasPermi('visit:item:edit')")
    @Log(title = "走访事项", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VisitItem item)
    {
        if (item.getItemId() == null)
        {
            return error("缺少事项ID");
        }
        return toAjax(visitItemService.updateVisitItem(item));
    }

    /** 删除事项（软删除） */
    @PreAuthorize("@ss.hasPermi('visit:item:remove')")
    @Log(title = "走访事项", businessType = BusinessType.DELETE)
    @DeleteMapping("/{itemId}")
    public AjaxResult remove(@PathVariable Long itemId)
    {
        return toAjax(visitItemService.deleteVisitItemById(itemId));
    }

    /** 事项跟进时间线 */
    @PreAuthorize("@ss.hasPermi('visit:item:query')")
    @GetMapping("/{itemId}/logs")
    public AjaxResult logs(@PathVariable Long itemId)
    {
        return success(visitItemService.selectItemLogs(itemId));
    }

    /** 手动新增跟进记录 */
    @PreAuthorize("@ss.hasPermi('visit:item:edit')")
    @Log(title = "走访事项跟进", businessType = BusinessType.INSERT)
    @PostMapping("/{itemId}/follow")
    public AjaxResult follow(@PathVariable Long itemId, @RequestBody java.util.Map<String, String> body)
    {
        String content = body == null ? null : body.get("content");
        if (content == null || content.isBlank())
        {
            return error("跟进内容不能为空");
        }
        return toAjax(visitItemService.addFollowLog(itemId, content));
    }

    /** 责任人候选列表（本学院辅导员，issue #11 第四阶段） */
    @PreAuthorize("@ss.hasPermi('visit:item:list')")
    @GetMapping("/assignees")
    public AjaxResult assignees()
    {
        return success(visitOrgService.assigneeCandidates());
    }

    /** 事项统计（各状态数量 + 超期数，issue #11 第五阶段） */
    @PreAuthorize("@ss.hasPermi('visit:item:list')")
    @GetMapping("/stats")
    public AjaxResult stats(VisitItem item)
    {
        return success(visitItemService.itemStats(item));
    }
}
