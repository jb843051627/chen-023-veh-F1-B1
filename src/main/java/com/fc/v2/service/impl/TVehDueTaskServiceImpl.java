package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TVehDueTaskMapper;
import com.fc.v2.model.auto.TVehDueTask;
import com.fc.v2.service.ITVehDueTaskService;

/**
 * 证照到期催办条目 Service业务层处理（scheduling-job 形状：周期执行）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TVehDueTaskServiceImpl implements ITVehDueTaskService {

    private static final int STATUS_WAIT = 0;
    private static final int STATUS_DONE = 1;

    @javax.annotation.Resource
    private TVehDueTaskMapper vehDueTaskMapper;

    @Override
    public TVehDueTask selectTVehDueTaskById(Long id) {
        return this.vehDueTaskMapper.selectById(id);
    }

    /**
     * 时段比较统一走**墙钟字符串**（yyyy-MM-dd HH:mm:ss）：JDBC 的 serverTimezone 与本机
     * 时区不对称，直接把 java.util.Date 作参数会整体偏移，使"恰好到期"这类边界用例错判。
     */
    private static String ts(Date d) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    @Override
    public List<TVehDueTask> listDue(Date at) {
        if (at == null) {
            return new ArrayList<TVehDueTask>();
        }
        // 只看待派、没删的条目；已派出的、分不出的不再回锅
        List<TVehDueTask> all = this.vehDueTaskMapper.selectList(new QueryWrapper<TVehDueTask>()
                .eq("del_flag", 0)
                .eq("status", STATUS_WAIT));
        List<TVehDueTask> due = new ArrayList<TVehDueTask>();
        for (TVehDueTask r : all) {
            // 到了止点那一刻就算到期（含边界），不能把恰好到期的漏下
            if (r.getDueAt() != null && ts(r.getDueAt()).compareTo(ts(at)) <= 0) {
                due.add(r);
            }
        }
        return due;
    }

    @Override
    public int runOnce(Date at) {
        List<TVehDueTask> due = listDue(at);
        // 一行都没有的时候安安稳稳返 0，不许伸指头去点头一行把程序点崩
        if (due.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TVehDueTask r : due) {
            // 单条缺数的跳过继续，别让一条空数连累整趟；办成的落"已派出"，下一趟不再派它
            if (r.getAmount() == null || r.getAmount().signum() < 0) {
                continue;
            }
            TVehDueTask upd = new TVehDueTask();
            upd.setId(r.getId());
            upd.setStatus(STATUS_DONE);
            if (this.vehDueTaskMapper.updateById(upd) > 0) {
                ok++;
            }
        }
        return ok;
    }
}
