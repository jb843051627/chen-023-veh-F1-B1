package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TVehBindPageMapper;
import com.fc.v2.mapper.auto.TVehEntMapper;
import com.fc.v2.model.auto.TVehBindPage;
import com.fc.v2.model.auto.TVehEnt;
import com.fc.v2.service.ITVehBindPageService;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 车辆联网清单页Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TVehBindPageServiceImpl extends ServiceImpl<TVehBindPageMapper, TVehBindPage> implements ITVehBindPageService {

    @Autowired
    private TVehEntMapper vehEntMapper;

    @Override
    public TVehBindPage selectTVehBindPageById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TVehBindPage>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TVehBindPage> selectTVehBindPageList(Wrapper<TVehBindPage> queryWrapper) {
        // 册上什么情形都摆出来，筛选条件照调用方递来的走；不另起一页、不另卡一个情形
        QueryWrapper<TVehBindPage> wrapper = queryWrapper instanceof QueryWrapper
                ? ((QueryWrapper<TVehBindPage>) queryWrapper)
                : new QueryWrapper<TVehBindPage>();
        wrapper.eq("del_flag", 0);
        return this.baseMapper.selectList(wrapper);
    }

    /** 运输业户总名录的"在册"口径，挂接/改挂两处统一走这一处：未删且名录情形为在册（0） */
    private TVehEnt resolveActiveEnt(Integer siteId) {
        if (siteId == null) {
            return null;
        }
        return vehEntMapper.selectOne(new QueryWrapper<TVehEnt>()
                .eq("id", siteId)
                .eq("del_flag", 0)
                .eq("status", 0));
    }

    /** 还差几项 = 应联项数 - 已联项数；换算只此一处，新增、修改同一算法，余额才对得齐 */
    private int calcLack(Integer should, Integer done) {
        return (should == null ? 0 : should) - (done == null ? 0 : done);
    }

    @Override
    public int insertTVehBindPage(TVehBindPage record) {
        if (record == null) {
            return 0;
        }

        record.setCreateBy(record.getBillNo());
        TVehEnt refArch = resolveActiveEnt(record.getSiteId());
        if (refArch == null) {
            // 名录上对不上、或是已销户的业户，不许挂接
            return 0;
        }
        record.setSiteNo(refArch.getSiteNo());
        if (StringUtils.isNotEmpty(record.getBillNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TVehBindPage>()
                    .eq("bill_no", record.getBillNo()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }
        record.setLackCount(calcLack(record.getShouldCount(), record.getDoneCount()));

        record.setDelFlag(0);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTVehBindPage(TVehBindPage record) {
        if (record == null || record.getId() == null) {
            return 0;
        }

        TVehBindPage old = this.baseMapper.selectOne(new QueryWrapper<TVehBindPage>()
                .eq("id", record.getId()).eq("del_flag", 0));
        if (old == null) {
            return 0;
        }

        if (StringUtils.isNotEmpty(record.getBillNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TVehBindPage>()
                    .eq("bill_no", record.getBillNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

        // 改挂业户的，按总名录同一套在册口径重新核，并把业户代号带成名录上的
        if (record.getSiteId() != null) {
            TVehEnt refArch = resolveActiveEnt(record.getSiteId());
            if (refArch == null) {
                return 0;
            }
            record.setSiteNo(refArch.getSiteNo());
        }

        // 换算与新增同一口径：缺省的数沿用旧值再算余额，保证结存对得齐
        Integer should = record.getShouldCount() != null ? record.getShouldCount() : old.getShouldCount();
        Integer done = record.getDoneCount() != null ? record.getDoneCount() : old.getDoneCount();
        record.setLackCount(calcLack(should, done));

        record.setUpdateTime(new Date());
        return this.baseMapper.update(record, new UpdateWrapper<TVehBindPage>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
    }

    @Override
    public int deleteTVehBindPageByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    @Override
    public int deleteTVehBindPageById(Long id) {
        return this.baseMapper.deleteById(id);
    }
}
