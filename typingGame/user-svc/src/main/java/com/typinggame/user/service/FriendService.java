package com.typinggame.user.service;

import com.typinggame.dto.friend.FriendRequestDTO;
import com.typinggame.dto.friend.FriendResponseDTO;

import java.util.List;

public interface FriendService {
    List<FriendResponseDTO> getFriendList(Integer userId);

    List<FriendResponseDTO> searchUsers(Integer userId, String keyword);

    FriendRequestDTO sendFriendRequest(Integer senderId, Integer receiverId);

    boolean acceptFriendRequest(Integer requestId);

    boolean rejectFriendRequest(Integer requestId);

    boolean deleteFriend(Integer userId, Integer friendId);

    List<FriendRequestDTO> getFriendRequests(Integer userId);
}