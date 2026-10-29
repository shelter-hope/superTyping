package com.typinggame.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 批量查询用户档案的请求体。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfilesQueryDTO {
    private List<Integer> userIds;
}
