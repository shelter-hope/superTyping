package com.typinggame.user.service.impl;

import com.typinggame.dto.friend.FriendRequestDTO;
import com.typinggame.dto.friend.FriendResponseDTO;
import com.typinggame.entity.message.FriendRelation;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.enums.FriendRelationStatus;
import com.typinggame.user.im.FriendEventPublisher;
import com.typinggame.user.repository.FriendRelationRepository;
import com.typinggame.user.repository.UserInfoRepository;
import com.typinggame.user.service.FriendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FriendServiceImpl implements FriendService {

    @Autowired
    private FriendRelationRepository friendRelationRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private FriendEventPublisher friendEventPublisher;

    @Override
    public List<FriendResponseDTO> getFriendList(Integer userId) {
        List<FriendRelation> friendRelations = friendRelationRepository.findByUserIdOrFriendUserIdAndRelationStatus(userId, userId, FriendRelationStatus.FRIEND.getCode());
        return friendRelations.stream().map(relation -> {
            Integer friendId = relation.getUserId().equals(userId) ? relation.getFriendUserId() : relation.getUserId();
            Optional<UserInfo> friendOptional = userInfoRepository.findById(friendId);
            if (friendOptional.isPresent()) {
                UserInfo friend = friendOptional.get();
                FriendResponseDTO friendDTO = new FriendResponseDTO();
                friendDTO.setId(friend.getUserId());
                friendDTO.setUserId(friend.getUserId());
                friendDTO.setUsername(friend.getUsername());
                friendDTO.setAvatar(friend.getAvatar());
                friendDTO.setBio(friend.getSignature());
                friendDTO.setIsOnline(false);
                friendDTO.setLastActiveTime(friend.getUpdateTime().toString());
                return friendDTO;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<FriendResponseDTO> searchUsers(Integer userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String k = keyword.trim();
        List<UserInfo> users = userInfoRepository.findByUsernameContainingOrNicknameContaining(k, k, PageRequest.of(0, 50)).getContent();
        return users.stream().filter(user -> !user.getUserId().equals(userId)).map(user -> {
            FriendResponseDTO friendDTO = new FriendResponseDTO();
            friendDTO.setId(user.getUserId());
            friendDTO.setUserId(user.getUserId());
            friendDTO.setUsername(user.getUsername());
            friendDTO.setAvatar(user.getAvatar());
            friendDTO.setBio(user.getSignature());
            friendDTO.setIsOnline(false);
            friendDTO.setLastActiveTime(user.getUpdateTime().toString());
            return friendDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public FriendRequestDTO sendFriendRequest(Integer senderId, Integer receiverId) {
        Optional<FriendRelation> existingRelation = friendRelationRepository.findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(senderId, receiverId, receiverId, senderId);
        if (existingRelation.isPresent()) {
            return null;
        }

        FriendRelation friendRelation = new FriendRelation();
        friendRelation.setUserId(senderId);
        friendRelation.setFriendUserId(receiverId);
        friendRelation.setRelationStatus(FriendRelationStatus.PENDING.getCode());
        LocalDateTime now = LocalDateTime.now();
        friendRelation.setApplyTime(now);
        friendRelation.setCreateTime(now);
        friendRelationRepository.save(friendRelation);
        friendEventPublisher.friendRequest(senderId, receiverId, friendRelation.getId());

        Optional<UserInfo> senderOptional = userInfoRepository.findById(senderId);
        Optional<UserInfo> receiverOptional = userInfoRepository.findById(receiverId);
        if (senderOptional.isPresent() && receiverOptional.isPresent()) {
            FriendRequestDTO requestDTO = new FriendRequestDTO();
            requestDTO.setId(friendRelation.getId());
            requestDTO.setSenderId(senderId);
            requestDTO.setSenderName(senderOptional.get().getUsername());
            requestDTO.setReceiverId(receiverId);
            requestDTO.setReceiverName(receiverOptional.get().getUsername());
            requestDTO.setSendTime(friendRelation.getCreateTime());
            requestDTO.setStatus(FriendRelationStatus.getByCode(friendRelation.getRelationStatus()).getDesc());
            return requestDTO;
        }
        return null;
    }

    @Override
    public boolean acceptFriendRequest(Integer requestId) {
        Optional<FriendRelation> relationOptional = friendRelationRepository.findById(requestId);
        if (relationOptional.isPresent()) {
            FriendRelation relation = relationOptional.get();
            relation.setRelationStatus(FriendRelationStatus.FRIEND.getCode());
            relation.setUpdateTime(LocalDateTime.now());
            friendRelationRepository.save(relation);
            return true;
        }
        return false;
    }

    @Override
    public boolean rejectFriendRequest(Integer requestId) {
        Optional<FriendRelation> relationOptional = friendRelationRepository.findById(requestId);
        if (relationOptional.isPresent()) {
            FriendRelation relation = relationOptional.get();
            relation.setRelationStatus(FriendRelationStatus.REJECTED.getCode());
            relation.setUpdateTime(LocalDateTime.now());
            friendRelationRepository.save(relation);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteFriend(Integer userId, Integer friendId) {
        Optional<FriendRelation> relationOptional = friendRelationRepository.findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(userId, friendId, friendId, userId);
        if (relationOptional.isPresent()) {
            friendRelationRepository.delete(relationOptional.get());
            return true;
        }
        return false;
    }

    @Override
    public List<FriendRequestDTO> getFriendRequests(Integer userId) {
        List<FriendRelation> friendRequests = friendRelationRepository.findByFriendUserIdAndRelationStatus(userId, FriendRelationStatus.PENDING.getCode());
        return friendRequests.stream().map(request -> {
            Optional<UserInfo> senderOptional = userInfoRepository.findById(request.getUserId());
            Optional<UserInfo> receiverOptional = userInfoRepository.findById(request.getFriendUserId());
            if (senderOptional.isPresent() && receiverOptional.isPresent()) {
                FriendRequestDTO requestDTO = new FriendRequestDTO();
                requestDTO.setId(request.getId());
                requestDTO.setSenderId(request.getUserId());
                requestDTO.setSenderName(senderOptional.get().getUsername());
                requestDTO.setReceiverId(request.getFriendUserId());
                requestDTO.setReceiverName(receiverOptional.get().getUsername());
                requestDTO.setSendTime(request.getCreateTime());
                requestDTO.setStatus(FriendRelationStatus.getByCode(request.getRelationStatus()).getDesc());
                return requestDTO;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}