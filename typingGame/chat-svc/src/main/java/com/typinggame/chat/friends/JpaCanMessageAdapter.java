package com.typinggame.chat.friends;

import com.typinggame.chat.repository.FriendRelationRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaCanMessageAdapter implements CanMessagePort {
    private final FriendRelationRepository friendRelationRepository;

    public JpaCanMessageAdapter(FriendRelationRepository friendRelationRepository) {
        this.friendRelationRepository = friendRelationRepository;
    }

    @Override
    public boolean canMessage(Integer senderId, Integer receiverId) {
        return friendRelationRepository.findAccepted(senderId, receiverId).isPresent();
    }
}
