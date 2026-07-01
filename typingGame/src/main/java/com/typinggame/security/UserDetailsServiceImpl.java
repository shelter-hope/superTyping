package com.typinggame.security;

import com.typinggame.entity.user.UserInfo;
import com.typinggame.repository.UserInfoRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 用户详情服务实现
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserInfoRepository userInfoRepository;

    public UserDetailsServiceImpl(UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 尝试将username转换为Integer类型，作为用户ID
        Integer userId;
        try {
            userId = Integer.parseInt(username);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("无效的用户ID格式: " + username);
        }

        // 根据用户ID查找用户
        UserInfo userInfo = userInfoRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + userId));

        // 创建用户详情
        return new User(
                String.valueOf(userInfo.getUserId()),
                userInfo.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userInfo.getRole().toUpperCase()))
        );
    }
}
