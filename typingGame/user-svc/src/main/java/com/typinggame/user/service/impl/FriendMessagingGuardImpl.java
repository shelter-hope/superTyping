package com.typinggame.user.service.impl;

import com.typinggame.entity.message.FriendRelation;
import com.typinggame.enums.FriendRelationStatus;
import com.typinggame.user.repository.FriendRelationRepository;
import com.typinggame.user.service.FriendMessagingGuard;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class FriendMessagingGuardImpl implements FriendMessagingGuard {
    private final FriendRelationRepository friendRelationRepository;

    public FriendMessagingGuardImpl(FriendRelationRepository friendRelationRepository) {
        this.friendRelationRepository = friendRelationRepository;
    }

    @Override
    public void assertCanMessage(Integer senderId, Integer receiverId) {
        if (!canMessage(senderId, receiverId)) {
            throw new IllegalArgumentException("只能给好友发消息");
        }
    }

    @Override
    public boolean canMessage(Integer senderId, Integer receiverId) {
        if (senderId == null || receiverId == null || senderId.equals(receiverId)) {
            return false;
        }
        Optional<FriendRelation> relation = friendRelationRepository
                .findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(senderId, receiverId, receiverId, senderId);
        return relation
                .filter(r -> Objects.equals(r.getRelationStatus(), FriendRelationStatus.FRIEND.getCode()))
                .isPresent();
    }
}
