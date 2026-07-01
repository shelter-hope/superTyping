package com.typinggame.service;

import com.typinggame.dto.friend.FriendRequestDTO;
import com.typinggame.dto.friend.FriendResponseDTO;
import com.typinggame.entity.message.FriendRelation;

import java.util.List;

/**
 * 处理好友相关业务逻辑
 */
public interface FriendService {
    /**
     * 获取好友列表
     * @param userId 用户ID
     * @return 好友列表
     */
    List<FriendResponseDTO> getFriendList(Integer userId);

    /**
     * 搜索用户
     * @param userId 当前用户ID
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    List<FriendResponseDTO> searchUsers(Integer userId, String keyword);

    /**
     * 发送好友请求
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @return 好友请求DTO
     */
    FriendRequestDTO sendFriendRequest(Integer senderId, Integer receiverId);

    /**
     * 接受好友请求
     * @param requestId 请求ID
     * @return 操作结果
     */
    boolean acceptFriendRequest(Integer requestId);

    /**
     * 拒绝好友请求
     * @param requestId 请求ID
     * @return 操作结果
     */
    boolean rejectFriendRequest(Integer requestId);

    /**
     * 删除好友
     * @param userId 当前用户ID
     * @param friendId 好友ID
     * @return 操作结果
     */
    boolean deleteFriend(Integer userId, Integer friendId);

    /**
     * 获取好友请求列表
     * @param userId 用户ID
     * @return 好友请求列表
     */
    List<FriendRequestDTO> getFriendRequests(Integer userId);
}