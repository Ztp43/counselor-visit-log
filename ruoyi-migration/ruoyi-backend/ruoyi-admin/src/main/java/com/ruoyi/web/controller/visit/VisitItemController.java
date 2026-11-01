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
import com.ruoyi.visit.service.IVisitItemService;

/**
 * 走访事项台账（issue #11 事项/跟进闭环体系）
 */
@RestController
@RequestMapping("/visit/item")
public class VisitItemController extends BaseController
{
    private final IVisitItemService visitItemService;

    public VisitItemController(IVisitItemService visitItemService)
    {
        this.visitItemService = visitItemService;
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
        return toAjax(visitItemService.insertVisitItem(item));
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
}
