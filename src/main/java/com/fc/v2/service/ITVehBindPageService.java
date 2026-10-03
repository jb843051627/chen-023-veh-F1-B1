package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TVehBindPage;

import java.util.List;

/**
 * 车辆联网清单页 Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITVehBindPageService {

    /** 按主键查询 */
    TVehBindPage selectTVehBindPageById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TVehBindPage> selectTVehBindPageList(Wrapper<TVehBindPage> queryWrapper);

    /** 新增 */
    int insertTVehBindPage(TVehBindPage record);

    /** 修改 */
    int updateTVehBindPage(TVehBindPage record);

    /** 批量删除 */
    int deleteTVehBindPageByIds(String ids);

    /** 按主键删除 */
    int deleteTVehBindPageById(Long id);
}
