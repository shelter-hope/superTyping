package com.typinggame.user.service;

import com.typinggame.dto.friend.FriendRequestDTO;
import com.typinggame.dto.friend.FriendResponseDTO;

import java.util.List;

public interface FriendService {
    List<FriendResponseDTO> getFriendList(Integer userId);

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
