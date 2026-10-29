package com.typinggame.user.service;

import com.typinggame.dto.user.UserGameSummaryDTO;
import com.typinggame.dto.user.UserProfileDTO;

import java.util.List;

/**
 * 集群内用户数据访问：其它服务通过 HTTP 读取用户档案、回写对局汇总，
 * 不再直连 user_info 表（用户数据唯一归属 user-svc）。
 */
public interface InternalUserService {

    /** 用户档案；不存在返回 null。 */
    UserProfileDTO getProfile(Integer userId);

    /** 批量用户档案，保持入参顺序并跳过不存在的用户。 */
    List<UserProfileDTO> getProfiles(List<Integer> userIds);

    /** 全部用户 ID（聊天广播等场景）。 */
    List<Integer> getAllUserIds();

    boolean exists(Integer userId);

    /** 一局结束后的汇总回写：最佳成绩判定与总场数累加在 user-svc 内部完成。 */
    UserProfileDTO applyGameSummary(Integer userId, UserGameSummaryDTO summary);

    /** 按日期累加一局的对局统计；统计口径（加权均值、峰值、场次累加）在 user-svc 内部完成。 */
    void upsertDailyStats(Integer userId, com.typinggame.dto.user.DailyStatsUpdateDTO update);
}
