package com.typinggame.user.service.impl;

import com.typinggame.common.exception.BusinessException;
import com.typinggame.common.im.PresenceKeys;
import com.typinggame.dto.friend.FriendRequestDTO;
import com.typinggame.dto.friend.FriendResponseDTO;
import com.typinggame.user.entity.UserInfo;
import com.typinggame.enums.FriendRelationStatus;
import com.typinggame.user.entity.FriendRelation;
import com.typinggame.user.im.FriendEventPublisher;
import com.typinggame.user.repository.FriendRelationRepository;
import com.typinggame.user.repository.UserInfoRepository;
import com.typinggame.user.service.FriendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FriendServiceImpl implements FriendService {
    private static final int APPLY_MESSAGE_LIMIT = 200;

    @Autowired
    private FriendRelationRepository friendRelationRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private FriendEventPublisher friendEventPublisher;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public List<FriendResponseDTO> getFriendList(Integer userId) {
        return friendRelationRepository.findInvolving(userId, FriendRelationStatus.FRIEND.getCode()).stream()
                .map(relation -> toFriend(otherUserId(relation, userId)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> findFriendIds(Integer userId) {
        return friendRelationRepository.findFriendIdsByStatus(userId, FriendRelationStatus.FRIEND.getCode());
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
            friendDTO.setLastActiveTime(user.getUpdateTime() == null ? null : user.getUpdateTime().toString());
            return friendDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public FriendRequestDTO sendFriendRequest(Integer senderId, Integer receiverId, String applyMessage) {
        if (senderId.equals(receiverId)) {
            throw BusinessException.badRequest("不能添加自己");
        }
        UserInfo receiver = userInfoRepository.findById(receiverId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        String note = normalizeNote(applyMessage);
        Optional<FriendRelation> existingRelation = friendRelationRepository
                .findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(senderId, receiverId, receiverId, senderId);
        FriendRelation friendRelation;
        if (existingRelation.isPresent()) {
            friendRelation = existingRelation.get();
            int status = friendRelation.getRelationStatus() == null ? -1 : friendRelation.getRelationStatus();
            if (status == FriendRelationStatus.REJECTED.getCode()) {
                friendRelation.setUserId(senderId);
                friendRelation.setFriendUserId(receiverId);
                friendRelation.setRelationStatus(FriendRelationStatus.PENDING.getCode());
                friendRelation.setApplyMessage(note);
                friendRelation.setBlockedBy(null);
                friendRelation.setAgreeTime(null);
                friendRelation.setApplyTime(LocalDateTime.now());
                friendRelation.setUpdateTime(LocalDateTime.now());
                friendRelationRepository.save(friendRelation);
            } else if (status == FriendRelationStatus.PENDING.getCode()) {
                throw BusinessException.badRequest(senderId.equals(friendRelation.getUserId())
                        ? "已经发送过申请"
                        : "对方已向你发起申请");
            } else if (status == FriendRelationStatus.FRIEND.getCode()) {
                throw BusinessException.badRequest("你们已经是好友");
            } else {
                throw BusinessException.badRequest("无法添加该用户");
            }
        } else {
            friendRelation = new FriendRelation();
            friendRelation.setUserId(senderId);
            friendRelation.setFriendUserId(receiverId);
            friendRelation.setRelationStatus(FriendRelationStatus.PENDING.getCode());
            friendRelation.setApplyMessage(note);
            LocalDateTime now = LocalDateTime.now();
            friendRelation.setApplyTime(now);
            friendRelation.setCreateTime(now);
            friendRelationRepository.save(friendRelation);
        }
        friendEventPublisher.friendRequest(senderId, receiverId, friendRelation.getId(), note);
        UserInfo sender = userInfoRepository.findById(senderId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        return toRequest(friendRelation, sender, receiver);
    }

    @Override
    public boolean acceptFriendRequest(Integer operatorId, Integer requestId) {
        FriendRelation relation = requireIncoming(operatorId, requestId);
        relation.setRelationStatus(FriendRelationStatus.FRIEND.getCode());
        relation.setAgreeTime(LocalDateTime.now());
        relation.setUpdateTime(LocalDateTime.now());
        friendRelationRepository.save(relation);
        return true;
    }

    @Override
    public boolean rejectFriendRequest(Integer operatorId, Integer requestId) {
        FriendRelation relation = requireIncoming(operatorId, requestId);
        relation.setRelationStatus(FriendRelationStatus.REJECTED.getCode());
        relation.setUpdateTime(LocalDateTime.now());
        friendRelationRepository.save(relation);
        return true;
    }

    @Override
    public boolean deleteFriend(Integer userId, Integer friendId) {
        Optional<FriendRelation> relationOptional = friendRelationRepository
                .findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(userId, friendId, friendId, userId);
        if (relationOptional.isPresent()) {
            friendRelationRepository.delete(relationOptional.get());
            return true;
        }
        return false;
    }

    @Override
    public void blockFriend(Integer userId, Integer friendId) {
        FriendRelation relation = friendRelationRepository
                .findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(userId, friendId, friendId, userId)
                .orElseThrow(() -> BusinessException.badRequest("只能拉黑好友"));
        if (Objects.equals(relation.getRelationStatus(), FriendRelationStatus.BLOCKED.getCode())) {
            if (userId.equals(relation.getBlockedBy())) {
                return;
            }
            throw BusinessException.forbidden("对方已将你拉黑");
        }
        if (!Objects.equals(relation.getRelationStatus(), FriendRelationStatus.FRIEND.getCode())) {
            throw BusinessException.badRequest("只能拉黑好友");
        }
        relation.setRelationStatus(FriendRelationStatus.BLOCKED.getCode());
        relation.setBlockedBy(userId);
        relation.setUpdateTime(LocalDateTime.now());
        friendRelationRepository.save(relation);
    }

    @Override
    public void unblockFriend(Integer userId, Integer friendId) {
        FriendRelation relation = friendRelationRepository
                .findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(userId, friendId, friendId, userId)
                .orElseThrow(() -> BusinessException.notFound("拉黑记录不存在"));
        if (!Objects.equals(relation.getRelationStatus(), FriendRelationStatus.BLOCKED.getCode())) {
            throw BusinessException.badRequest("当前没有拉黑");
        }
        if (!userId.equals(relation.getBlockedBy())) {
            throw BusinessException.forbidden("只有拉黑方可以解除");
        }
        relation.setRelationStatus(FriendRelationStatus.FRIEND.getCode());
        relation.setBlockedBy(null);
        relation.setUpdateTime(LocalDateTime.now());
        friendRelationRepository.save(relation);
    }

    @Override
    public List<FriendResponseDTO> getBlockedList(Integer userId) {
        return friendRelationRepository.findInvolving(userId, FriendRelationStatus.BLOCKED.getCode()).stream()
                .filter(relation -> userId.equals(relation.getBlockedBy()))
                .map(relation -> toFriend(otherUserId(relation, userId)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<FriendRequestDTO> getFriendRequests(Integer userId) {
        List<FriendRelation> friendRequests = friendRelationRepository.findByFriendUserIdAndRelationStatus(userId, FriendRelationStatus.PENDING.getCode());
        return friendRequests.stream().map(request -> {
            Optional<UserInfo> senderOptional = userInfoRepository.findById(request.getUserId());
            Optional<UserInfo> receiverOptional = userInfoRepository.findById(request.getFriendUserId());
            if (senderOptional.isPresent() && receiverOptional.isPresent()) {
                return toRequest(request, senderOptional.get(), receiverOptional.get());
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private FriendRelation requireIncoming(Integer operatorId, Integer requestId) {
        FriendRelation relation = friendRelationRepository.findById(requestId)
                .orElseThrow(() -> BusinessException.notFound("申请不存在"));
        if (!operatorId.equals(relation.getFriendUserId())) {
            throw BusinessException.forbidden("只能处理发给自己的申请");
        }
        if (!Objects.equals(relation.getRelationStatus(), FriendRelationStatus.PENDING.getCode())) {
            throw BusinessException.badRequest("申请已处理");
        }
        return relation;
    }

    private FriendResponseDTO toFriend(Integer friendId) {
        return userInfoRepository.findById(friendId).map(friend -> {
            FriendResponseDTO friendDTO = new FriendResponseDTO();
            friendDTO.setId(friend.getUserId());
            friendDTO.setUserId(friend.getUserId());
            friendDTO.setUsername(friend.getUsername());
            friendDTO.setAvatar(friend.getAvatar());
            friendDTO.setBio(friend.getSignature());
            friendDTO.setIsOnline(online(friend.getUserId()));
            friendDTO.setLastActiveTime(friend.getUpdateTime() == null ? null : friend.getUpdateTime().toString());
            return friendDTO;
        }).orElse(null);
    }

    private FriendRequestDTO toRequest(FriendRelation relation, UserInfo sender, UserInfo receiver) {
        FriendRequestDTO requestDTO = new FriendRequestDTO();
        requestDTO.setId(relation.getId());
        requestDTO.setSenderId(sender.getUserId());
        requestDTO.setSenderName(sender.getUsername());
        requestDTO.setReceiverId(receiver.getUserId());
        requestDTO.setReceiverName(receiver.getUsername());
        requestDTO.setSendTime(relation.getApplyTime() != null ? relation.getApplyTime() : relation.getCreateTime());
        FriendRelationStatus status = FriendRelationStatus.getByCode(relation.getRelationStatus());
        requestDTO.setStatus(status == null ? "" : status.getDesc());
        requestDTO.setApplyMessage(relation.getApplyMessage());
        return requestDTO;
    }

    private static Integer otherUserId(FriendRelation relation, Integer userId) {
        return relation.getUserId().equals(userId) ? relation.getFriendUserId() : relation.getUserId();
    }

    private static String normalizeNote(String applyMessage) {
        if (applyMessage == null || applyMessage.isBlank()) {
            return null;
        }
        String note = applyMessage.trim();
        if (note.length() > APPLY_MESSAGE_LIMIT) {
            throw BusinessException.badRequest("附言不能超过 200 字");
        }
        return note;
    }

    private boolean online(Integer userId) {
        if (redisTemplate == null || userId == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PresenceKeys.online(userId)));
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
