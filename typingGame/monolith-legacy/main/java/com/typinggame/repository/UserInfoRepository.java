package com.typinggame.repository;

import com.typinggame.entity.user.UserInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 用户仓库
 */
// 注解+继承JpaRepository，即可拥有所有CRUD能力
@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo,Integer> {
    // JpaRepository<实体类, 主键类型>
    // 自带能力：增/删/改/查/分页/排序，无需写任何代码


    /**
     *
     * @param userId
     * @return
     */
    Optional<UserInfo> findByUserId(@NotBlank(message = "用户ID不能为空") Integer userId);

    /**
     *
     * @param username
     * @return
     */
    Optional<UserInfo> findByUsername( String username);

    /**
     *
     * @param keyword
     * @return
     */
    List<UserInfo> findByNicknameContaining(String keyword);

    /**
     * 搜索用户
     */
    Page<UserInfo> findByUsernameContainingOrNicknameContaining(String username, String nickname, Pageable pageable);
}
