package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TVehRunLineMapper;
import com.fc.v2.model.auto.TVehRunLine;
import com.fc.v2.service.ITVehRunLineService;

/**
 * 运行界次判定线 Service业务层处理（rule-eval 形状）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TVehRunLineServiceImpl implements ITVehRunLineService {

    /** 界的情形：在位 */
    private static final int LINE_ON = 0;

    /** 优先级降序；并列按 ruleCode 降序（确定性） */
    private static final Comparator<TVehRunLine> PRIORITY_THEN_CODE_DESC = new Comparator<TVehRunLine>() {
        @Override
        public int compare(TVehRunLine a, TVehRunLine b) {
            int pa = a.getPriority() == null ? 0 : a.getPriority();
            int pb = b.getPriority() == null ? 0 : b.getPriority();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            return cb.compareTo(ca);
        }
    };

    @javax.annotation.Resource
    private TVehRunLineMapper vehRunLineMapper;

    @Override
    public TVehRunLine selectTVehRunLineById(Long id) {
        return this.vehRunLineMapper.selectById(id);
    }

    /**
     * 一条界在 at 时刻算不算数，全册各处（列表/单规则/单条可用性/条数）只认这一个说法，
     * 不许一边只看删除印、另一边又看在位、再一边还看生效日子：
     * 没删（del_flag=0）、在位（status=0）、到了起用日、还没到交班日（交班日不含）。
     */
    private boolean inPosition(TVehRunLine r, Date at) {
        if (r == null || at == null) {
            return false;
        }
        if (r.getDelFlag() != null && r.getDelFlag() != 0) {
            return false;
        }
        if (r.getStatus() == null || r.getStatus() != LINE_ON) {
            return false;
        }
        if (r.getEffStart() != null && at.before(r.getEffStart())) {
            return false;
        }
        // 交班那一日不含：恰好落在交班日就算下了班
        if (r.getEffEnd() != null && !at.before(r.getEffEnd())) {
            return false;
        }
        return true;
    }

    @Override
    public List<TVehRunLine> listAvailable(Date at) {
        if (at == null) {
            return new ArrayList<TVehRunLine>();
        }
        List<TVehRunLine> all = this.vehRunLineMapper.selectList(new QueryWrapper<TVehRunLine>()
                .eq("del_flag", 0));
        List<TVehRunLine> avail = new ArrayList<TVehRunLine>();
        for (TVehRunLine r : all) {
            if (inPosition(r, at)) {
                avail.add(r);
            }
        }
        // 优先级高的先说话；evaluateTop 拿头一条也靠这个次序
        Collections.sort(avail, PRIORITY_THEN_CODE_DESC);
        return avail;
    }

    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        if (ruleCode == null || ruleCode.trim().isEmpty() || input == null || at == null) {
            return 0;
        }
        TVehRunLine rule = findRule(ruleCode, at);
        if (rule == null) {
            return 0;
        }
        return levelOf(rule, input);
    }

    @Override
    public int evaluateTop(BigDecimal input, Date at) {
        if (input == null || at == null) {
            return 0;
        }
        if (input.compareTo(BigDecimal.ZERO) < 0
                || input.compareTo(new BigDecimal("100")) > 0) {
            return 0;
        }
        List<TVehRunLine> avail = listAvailable(at);
        if (avail.isEmpty()) {
            return 0;
        }
        // 列表已按优先级排定，头一条就是该处优先级最高的界，与列表页看的是同一条
        return levelOf(avail.get(0), input);
    }

    @Override
    public boolean usable(Long id, Date at) {
        if (id == null || at == null) {
            return false;
        }
        // 与列表/单规则同一处口径：在位且在生效日子里才算可用
        return inPosition(this.vehRunLineMapper.selectById(id), at);
    }

    @Override
    public int countAvailable(Date at) {
        if (at == null) {
            return 0;
        }
        return listAvailable(at).size();
    }

    /** 按代号在 at 时刻生效的版本里挑交班日最晚的一条；没一条在日子里的就是没有 */
    private TVehRunLine findRule(String ruleCode, Date at) {
        List<TVehRunLine> hit = this.vehRunLineMapper.selectList(new QueryWrapper<TVehRunLine>()
                .eq("del_flag", 0).eq("rule_code", ruleCode));
        TVehRunLine newest = null;
        for (TVehRunLine r : hit) {
            if (!inPosition(r, at)) {
                continue;
            }
            if (newest == null || r.getEffEnd() == null) {
                newest = r;
            } else if (newest.getEffEnd() != null && r.getEffEnd().after(newest.getEffEnd())) {
                newest = r;
            }
        }
        return newest;
    }

    /**
     * 分档：等于上限取高一档——够着某一档的上限，就按下一档算。
     * 某档上限空着没落的，这一档不取，往下一档看；三档上限都没有的，判不出档，按不可用返 0。
     */
    private int levelOf(TVehRunLine rule, BigDecimal input) {
        if (rule.getTh1Max() != null && input.compareTo(rule.getTh1Max()) < 0) {
            return 1;
        }
        if (rule.getTh2Max() != null && input.compareTo(rule.getTh2Max()) < 0) {
            return 2;
        }
        if (rule.getTh3Max() != null && input.compareTo(rule.getTh3Max()) < 0) {
            return 3;
        }
        if (rule.getTh1Max() == null && rule.getTh2Max() == null && rule.getTh3Max() == null) {
            return 0;
        }
        return 4;
    }
}
