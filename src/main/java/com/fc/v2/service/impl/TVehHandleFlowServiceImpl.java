package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TVehHandleFlowMapper;
import com.fc.v2.model.auto.TVehHandleFlow;
import com.fc.v2.service.ITVehHandleFlowService;

/**
 * 严重报警处置单 Service业务层处理（state-machine 形状：单据流转）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TVehHandleFlowServiceImpl implements ITVehHandleFlowService {

    private static final int MAX_STAGE = 3;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_TERMINAL = 2;

    @javax.annotation.Resource
    private TVehHandleFlowMapper vehHandleFlowMapper;

    @Override
    public TVehHandleFlow selectTVehHandleFlowById(Long id) {
        return this.vehHandleFlowMapper.selectById(id);
    }

    @Override
    public List<TVehHandleFlow> selectTVehHandleFlowList(QueryWrapper<TVehHandleFlow> queryWrapper) {
        return this.vehHandleFlowMapper.selectList(queryWrapper);
    }

    @Override
    public TVehHandleFlow advance(Long id, String remark) {
        TVehHandleFlow r = this.vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        // 已封尾（归档）的单子哪儿都不许再挪：推进、回退、改内容、删除同一套说法
        if (isTerminal(r)) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        // 一次只推进一档，到顶（归档）即封尾；不许跳档
        int next = Math.min(st + 1, MAX_STAGE);
        r.setStage(next);
        r.setStatus(next >= MAX_STAGE ? STATUS_TERMINAL : STATUS_ACTIVE);
        r.setLastAction(remark);
        this.vehHandleFlowMapper.updateById(r);
        return r;
    }

    @Override
    public TVehHandleFlow rollback(Long id, String remark) {
        TVehHandleFlow r = this.vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        if (isTerminal(r)) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        // 一次只回退一档，退回起点即为"未起"，不与"在办"混着判
        int prev = Math.max(0, st - 1);
        r.setStage(prev);
        r.setStatus(prev == 0 ? 0 : STATUS_ACTIVE);
        r.setLastAction(remark);
        this.vehHandleFlowMapper.updateById(r);
        return r;
    }

    /** 同一套"已封尾"口径：推进/回退/改内容/删除都拿这一处核 */
    private boolean isTerminal(TVehHandleFlow r) {
        Integer st = r.getStatus();
        Integer stage = r.getStage();
        return (st != null && st == STATUS_TERMINAL)
                || (stage != null && stage >= MAX_STAGE);
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TVehHandleFlow r = this.vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        // 已封尾不得再改
        if (isTerminal(r)) {
            return false;
        }
        r.setContent(remark);
        return this.vehHandleFlowMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TVehHandleFlow r = this.vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        // 已封尾不得删除
        if (isTerminal(r)) {
            return false;
        }
        return this.vehHandleFlowMapper.deleteById(id) > 0;
    }

}
