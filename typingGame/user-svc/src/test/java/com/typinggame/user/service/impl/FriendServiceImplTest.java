package com.typinggame.user.service.impl;

import com.typinggame.common.exception.BusinessException;
import com.typinggame.common.im.PresenceKeys;
import com.typinggame.dto.friend.FriendRequestDTO;
import com.typinggame.dto.friend.FriendResponseDTO;
import com.typinggame.entity.message.FriendRelation;
import com.typinggame.entity.user.UserInfo;
import com.typinggame.enums.FriendRelationStatus;
import com.typinggame.user.im.FriendEventPublisher;
import com.typinggame.user.repository.FriendRelationRepository;
import com.typinggame.user.repository.UserInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendServiceImplTest {

    @Mock private FriendRelationRepository friendRelationRepository;
    @Mock private UserInfoRepository userInfoRepository;
    @Mock private FriendEventPublisher friendEventPublisher;
    @Mock private StringRedisTemplate redisTemplate;
    @InjectMocks private FriendServiceImpl service;

    @Test
    void storesApplyMessage() {
        when(userInfoRepository.findById(5)).thenReturn(Optional.of(user(5, "bob")));
        when(userInfoRepository.findById(3)).thenReturn(Optional.of(user(3, "ann")));
        when(friendRelationRepository.findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(3, 5, 5, 3))
                .thenReturn(Optional.empty());
        when(friendRelationRepository.save(any())).thenAnswer(invocation -> {
            FriendRelation relation = invocation.getArgument(0);
            relation.setId(8);
            return relation;
        });

        FriendRequestDTO dto = service.sendFriendRequest(3, 5, "  一起练打字  ");

        ArgumentCaptor<FriendRelation> saved = ArgumentCaptor.forClass(FriendRelation.class);
        verify(friendRelationRepository).save(saved.capture());
        assertEquals("一起练打字", saved.getValue().getApplyMessage());
        assertEquals("一起练打字", dto.getApplyMessage());
        assertEquals(FriendRelationStatus.PENDING.getCode(), saved.getValue().getRelationStatus());
    }

    @Test
    void onlyReceiverCanAccept() {
        FriendRelation relation = pending();
        when(friendRelationRepository.findById(8)).thenReturn(Optional.of(relation));

        BusinessException error = assertThrows(BusinessException.class, () -> service.acceptFriendRequest(3, 8));

        assertEquals(403, error.getCode());
        assertEquals(FriendRelationStatus.PENDING.getCode(), relation.getRelationStatus());
    }

    @Test
    void blockRemovesAcceptedRelationAndOnlyBlockerCanUndo() {
        FriendRelation relation = new FriendRelation();
        relation.setUserId(3);
        relation.setFriendUserId(5);
        relation.setRelationStatus(FriendRelationStatus.FRIEND.getCode());
        when(friendRelationRepository.findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(3, 5, 5, 3))
                .thenReturn(Optional.of(relation));
        when(friendRelationRepository.findByUserIdAndFriendUserIdOrUserIdAndFriendUserId(5, 3, 3, 5))
                .thenReturn(Optional.of(relation));

        service.blockFriend(3, 5);

        assertEquals(FriendRelationStatus.BLOCKED.getCode(), relation.getRelationStatus());
        assertEquals(3, relation.getBlockedBy());
        BusinessException error = assertThrows(BusinessException.class, () -> service.unblockFriend(5, 3));
        assertEquals(403, error.getCode());
        assertEquals(FriendRelationStatus.BLOCKED.getCode(), relation.getRelationStatus());

        service.unblockFriend(3, 5);

        assertEquals(FriendRelationStatus.FRIEND.getCode(), relation.getRelationStatus());
        assertNull(relation.getBlockedBy());
    }

    @Test
    void friendListReadsPresenceKey() {
        FriendRelation relation = new FriendRelation();
        relation.setUserId(3);
        relation.setFriendUserId(5);
        relation.setRelationStatus(FriendRelationStatus.FRIEND.getCode());
        when(friendRelationRepository.findInvolving(3, FriendRelationStatus.FRIEND.getCode())).thenReturn(List.of(relation));
        when(userInfoRepository.findById(5)).thenReturn(Optional.of(user(5, "bob")));
        when(redisTemplate.hasKey(PresenceKeys.online(5))).thenReturn(true);

        List<FriendResponseDTO> friends = service.getFriendList(3);

        assertEquals(1, friends.size());
        assertTrue(friends.get(0).getIsOnline());
    }

    private static FriendRelation pending() {
        FriendRelation relation = new FriendRelation();
        relation.setId(8);
        relation.setUserId(3);
        relation.setFriendUserId(5);
        relation.setRelationStatus(FriendRelationStatus.PENDING.getCode());
        return relation;
    }

    private static UserInfo user(int id, String name) {
        UserInfo user = new UserInfo();
        user.setUserId(id);
        user.setUsername(name);
        return user;
    }
}
