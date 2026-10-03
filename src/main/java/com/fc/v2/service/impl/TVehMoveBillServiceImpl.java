package com.fc.v2.service.impl;

import org.springframework.stereotype.Service;

import com.fc.v2.mapper.auto.TVehMoveBillMapper;
import com.fc.v2.model.auto.TVehMoveBill;
import com.fc.v2.service.ITVehMoveBillService;

/**
 * 移交执法核签单 Service业务层处理（approval-chain 形状：多阶段签批）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TVehMoveBillServiceImpl implements ITVehMoveBillService {

    /** 档口：初核/复核/核准 0..2 */
    private static final int MAX_NODE = 2;
    private static final int MODE_OR = 0;
    private static final int MODE_AND = 1;
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_PASS = 1;
    private static final int STATUS_VETO = 2;

    @javax.annotation.Resource
    private TVehMoveBillMapper vehMoveBillMapper;

    @Override
    public TVehMoveBill selectTVehMoveBillById(Long id) {
        return this.vehMoveBillMapper.selectById(id);
    }

    @Override
    public TVehMoveBill approve(Long id, String approver, String comment) {
        TVehMoveBill r = this.vehMoveBillMapper.selectById(id);
        if (r == null || approver == null || approver.trim().isEmpty()) {
            return null;
        }
        // 办结的、挪回的单不再落印
        if (r.getStatus() != null && r.getStatus() != STATUS_RUNNING) {
            return null;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        if (node > MAX_NODE) {
            return null;
        }
        // 本档落印先记数，结存（已落印/应落人数）两处都靠这一个数，不许只挪档不点数
        int signed = (r.getSignCount() == null ? 0 : r.getSignCount()) + 1;
        r.setSignCount(signed);
        if (nodeFilled(r, signed)) {
            // 本档算齐才许挪下一档：挪档同时把本档落印结清零，下一档从头点
            if (node >= MAX_NODE) {
                r.setStatus(STATUS_PASS);
            } else {
                r.setNodeNo(node + 1);
                r.setSignCount(0);
                r.setStatus(STATUS_RUNNING);
            }
        }
        return this.vehMoveBillMapper.updateById(r) > 0 ? r : null;
    }

    /** 本档算不算齐：一名即可模式落一人便齐；两名同判模式按应落人数点齐 */
    private boolean nodeFilled(TVehMoveBill r, int signed) {
        Integer mode = r.getSignMode();
        if (mode == null || mode == MODE_OR) {
            return signed >= 1;
        }
        if (mode == MODE_AND) {
            int need = r.getNeedCount() == null ? 0 : r.getNeedCount();
            return need > 0 && signed >= need;
        }
        return false;
    }

    @Override
    public TVehMoveBill reject(Long id, String approver, String comment) {
        TVehMoveBill r = this.vehMoveBillMapper.selectById(id);
        if (r == null || approver == null || approver.trim().isEmpty()) {
            return null;
        }
        if (r.getStatus() != null && r.getStatus() != STATUS_RUNNING) {
            return null;
        }
        // 否决即挪回，册上得落个"已挪回"的印，不许原样空写一遍
        r.setStatus(STATUS_VETO);
        return this.vehMoveBillMapper.updateById(r) > 0 ? r : null;
    }

    @Override
    public TVehMoveBill rollback(Long id, String comment) {
        TVehMoveBill r = this.vehMoveBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        if (r.getStatus() != null && r.getStatus() != STATUS_RUNNING) {
            return null;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        // 一档一档退；退回的档落印结清零，从头重点
        r.setNodeNo(Math.max(0, node - 1));
        r.setSignCount(0);
        return this.vehMoveBillMapper.updateById(r) > 0 ? r : null;
    }
}
