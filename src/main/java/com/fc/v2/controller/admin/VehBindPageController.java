package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TVehBindPage;
import com.fc.v2.service.ITVehBindPageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 车辆联网清单页 Controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "车辆联网清单页")
@Controller
@RequestMapping("/VehBindPageController")
public class VehBindPageController extends BaseController {

    private final String prefix = "admin/vehBindPage";

    @Autowired
    private ITVehBindPageService vehBindPageService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("veh:vehBindPage:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "车辆联网清单页集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("veh:vehBindPage:list")
    @ResponseBody
    public ResultTable list(TVehBindPage record) {
        QueryWrapper<TVehBindPage> queryWrapper = new QueryWrapper<TVehBindPage>();
        startPage();
        com.github.pagehelper.PageInfo<TVehBindPage> page =
                new com.github.pagehelper.PageInfo<TVehBindPage>(vehBindPageService.selectTVehBindPageList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "车辆联网清单页新增", action = "add")
    @ApiOperation(value = "新增", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("veh:vehBindPage:add")
    @ResponseBody
    public AjaxResult add(TVehBindPage record) {
        return toAjax(vehBindPageService.insertTVehBindPage(record));
    }

    @Log(title = "车辆联网清单页修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("veh:vehBindPage:edit")
    @ResponseBody
    public AjaxResult editSave(TVehBindPage record) {
        return toAjax(vehBindPageService.updateTVehBindPage(record));
    }

    @Log(title = "车辆联网清单页删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("veh:vehBindPage:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(vehBindPageService.deleteTVehBindPageByIds(ids));
    }
}
