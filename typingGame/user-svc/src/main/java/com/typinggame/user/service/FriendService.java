package com.typinggame.user.service;

import com.typinggame.dto.friend.FriendRequestDTO;
import com.typinggame.dto.friend.FriendResponseDTO;

import java.util.List;

public interface FriendService {
    List<FriendResponseDTO> getFriendList(Integer userId);

    /** 好友 ID 列表（仅接受状态），供 chat-svc 等内部服务调用。 */
    List<Integer> findFriendIds(Integer userId);

    List<FriendResponseDTO> searchUsers(Integer userId, String keyword);

    FriendRequestDTO sendFriendRequest(Integer senderId, Integer receiverId, String applyMessage);

    boolean acceptFriendRequest(Integer operatorId, Integer requestId);

    boolean rejectFriendRequest(Integer operatorId, Integer requestId);

    boolean deleteFriend(Integer userId, Integer friendId);

    void blockFriend(Integer userId, Integer friendId);

    void unblockFriend(Integer userId, Integer friendId);

    List<FriendResponseDTO> getBlockedList(Integer userId);

    List<FriendRequestDTO> getFriendRequests(Integer userId);
}
