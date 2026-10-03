package com.fc.v2.service.impl;

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

    /** 业户在名录里的在册情形 */
    private static final int ENT_ACTIVE = 0;
    /** 清单页新录情形 */
    private static final int PAGE_NEW = 0;

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
        // 调用方递来的条件照旧用，分页也归调用方统一起；这里只把"已删的不摆"这一条口径补齐，
        // 不另起炉灶硬塞进展情形、硬安一页十行，免得列表与回查两处看的不是一批数据
        QueryWrapper<TVehBindPage> wrapper = (QueryWrapper<TVehBindPage>) queryWrapper;
        wrapper.eq("del_flag", 0);
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * 运输业户总名录"在册"口径，新增、修改两处都只认这一个说法：
     * 名录里找得着、没销户（del_flag=0 且 status=0）才算数；
     * 已销户（status=1）、情形空着没落的、名录里没这条的，一律不认。
     */
    private TVehEnt selectActiveEnt(Integer siteId) {
        if (siteId == null) {
            return null;
        }
        TVehEnt ent = vehEntMapper.selectOne(new QueryWrapper<TVehEnt>()
                .eq("id", siteId)
                .eq("del_flag", 0));
        if (ent == null || ent.getStatus() == null || ent.getStatus() != ENT_ACTIVE) {
            return null;
        }
        return ent;
    }

    /** 还差几项 = 应联项数 − 已联项数；换算只此一处，新增与修改照同一个算式结存 */
    private int lackOf(Integer shouldCount, Integer doneCount) {
        int should = shouldCount == null ? 0 : shouldCount;
        int done = doneCount == null ? 0 : doneCount;
        return should - done;
    }

    @Override
    public int insertTVehBindPage(TVehBindPage record) {
        if (record == null) {
            return 0;
        }

        TVehEnt refArch = selectActiveEnt(record.getSiteId());
        if (refArch == null) {
            return 0;
        }
        // 业户代号不许由页上自带，一律按总名录带过来，免得同户两头两个代号
        record.setSiteNo(refArch.getSiteNo());
        if (record.getBillNo() != null && !record.getBillNo().trim().isEmpty()) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TVehBindPage>()
                    .eq("bill_no", record.getBillNo()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }
        record.setLackCount(lackOf(record.getShouldCount(), record.getDoneCount()));
        if (record.getStatus() == null) {
            record.setStatus(PAGE_NEW);
        }
        record.setDelFlag(0);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTVehBindPage(TVehBindPage record) {
        if (record == null || record.getId() == null) {
            return 0;
        }

        TVehBindPage live = this.baseMapper.selectOne(new QueryWrapper<TVehBindPage>()
                .eq("id", record.getId()).eq("del_flag", 0));
        if (live == null) {
            return 0;
        }

        if (record.getBillNo() != null && !record.getBillNo().trim().isEmpty()) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TVehBindPage>()
                    .eq("bill_no", record.getBillNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

        // 换挂业户时与新增走同一道门：在册才准挂，代号按名录带
        if (record.getSiteId() != null) {
            TVehEnt refArch = selectActiveEnt(record.getSiteId());
            if (refArch == null) {
                return 0;
            }
            record.setSiteNo(refArch.getSiteNo());
        }
        // 两个数递来了就按同一算式重结余额；只递一个时，另一个沿用册上原来的数，
        // 不许把没递的当 0，也不许新增一套减法、修改另一套算法
        if (record.getShouldCount() != null || record.getDoneCount() != null) {
            Integer should = record.getShouldCount() != null ? record.getShouldCount() : live.getShouldCount();
            Integer done = record.getDoneCount() != null ? record.getDoneCount() : live.getDoneCount();
            record.setLackCount(lackOf(should, done));
        }

        record.setUpdateTime(new Date());
        return this.baseMapper.update(record, new UpdateWrapper<TVehBindPage>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
    }

    @Override
    public int deleteTVehBindPageByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        // 册页只准软删：落个删除印，行子还在库里，与各处 del_flag=0 的口径接得上
        TVehBindPage upd = new TVehBindPage();
        upd.setDelFlag(1);
        return this.baseMapper.update(upd, new UpdateWrapper<TVehBindPage>()
                .in("id", Arrays.asList(idArr))
                .eq("del_flag", 0));
    }

    @Override
    public int deleteTVehBindPageById(Long id) {
        TVehBindPage upd = new TVehBindPage();
        upd.setDelFlag(1);
        return this.baseMapper.update(upd, new UpdateWrapper<TVehBindPage>()
                .eq("id", id)
                .eq("del_flag", 0));
    }
}
