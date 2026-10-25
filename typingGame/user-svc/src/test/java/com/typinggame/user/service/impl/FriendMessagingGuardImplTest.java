package com.typinggame.user.service.impl;

import com.typinggame.entity.message.FriendRelation;
import com.typinggame.enums.FriendRelationStatus;
import com.typinggame.user.repository.FriendRelationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendMessagingGuardImplTest {

    @Mock private FriendRelationRepository friendRelationRepository;
    @InjectMocks private FriendMessagingGuardImpl guard;

    @Test
    void friendsCanMessage() {
        FriendRelation relation = new FriendRelation();
        relation.setRelationStatus(FriendRelationStatus.FRIEND.getCode());
        when(friendRelationRepository.findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(3, 5, 5, 3))
                .thenReturn(Optional.of(relation));

        assertTrue(guard.canMessage(3, 5));
        guard.assertCanMessage(3, 5);
    }

    @Test
    void blockedOrMissingCannotMessage() {
        FriendRelation blocked = new FriendRelation();
        blocked.setRelationStatus(FriendRelationStatus.BLOCKED.getCode());
        when(friendRelationRepository.findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(3, 5, 5, 3))
                .thenReturn(Optional.of(blocked));
        assertFalse(guard.canMessage(3, 5));
        assertThrows(IllegalArgumentException.class, () -> guard.assertCanMessage(3, 5));

        when(friendRelationRepository.findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(3, 9, 9, 3))
                .thenReturn(Optional.empty());
        assertFalse(guard.canMessage(3, 9));
    }
}
