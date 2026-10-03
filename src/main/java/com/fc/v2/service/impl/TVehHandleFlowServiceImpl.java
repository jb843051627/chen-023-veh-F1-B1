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

    /** 步次：定级/知会/回告/归档 0..3 */
    private static final int MAX_STAGE = 3;
    private static final int STATUS_ACTIVE = 1;
    /** 已封尾（归档步次走完） */
    private static final int STATUS_TERMINAL = 2;

    @javax.annotation.Resource
    private TVehHandleFlowMapper vehHandleFlowMapper;

    @Override
    public TVehHandleFlow selectTVehHandleFlowById(Long id) {
        return this.vehHandleFlowMapper.selectById(id);
    }

    @Override
    public List<TVehHandleFlow> selectTVehHandleFlowList(QueryWrapper<TVehHandleFlow> queryWrapper) {
        // 台账只摆没删的单；软删掉的不入台账
        return this.vehHandleFlowMapper.selectList(queryWrapper.eq("del_flag", 0));
    }

    @Override
    public TVehHandleFlow advance(Long id, String remark) {
        TVehHandleFlow r = this.vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        // 已封尾的单子一步都不许再挪，台账报上去的步次不能在封尾后又变
        if (r.getStatus() != null && r.getStatus() == STATUS_TERMINAL) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        // 一次只准推进一档；走到归档那档就封尾。这数错一档，整张台账都得重算
        if (st >= MAX_STAGE) {
            return null;
        }
        r.setStage(st + 1);
        r.setStatus(r.getStage() == MAX_STAGE ? STATUS_TERMINAL : STATUS_ACTIVE);
        r.setLastAction(remark);
        return this.vehHandleFlowMapper.updateById(r) > 0 ? r : null;
    }

    @Override
    public TVehHandleFlow rollback(Long id, String remark) {
        TVehHandleFlow r = this.vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        if (r.getStatus() != null && r.getStatus() == STATUS_TERMINAL) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        // 回退也是一档一档退，不许一脚踹回起头
        r.setStage(Math.max(0, st - 1));
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        return this.vehHandleFlowMapper.updateById(r) > 0 ? r : null;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TVehHandleFlow r = this.vehHandleFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        // 已封尾不得再改
        if (r.getStatus() != null && r.getStatus() == STATUS_TERMINAL) {
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
        // 已封尾不得删除；只准落删除印的软删，台账上留得住痕
        if (r.getStatus() != null && r.getStatus() == STATUS_TERMINAL) {
            return false;
        }
        TVehHandleFlow upd = new TVehHandleFlow();
        upd.setId(id);
        upd.setDelFlag(1);
        return this.vehHandleFlowMapper.updateById(upd) > 0;
    }

}
