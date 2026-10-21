package com.typinggame.chat.service.impl;

import com.typinggame.chat.repository.CustomDictionaryRepository;
import com.typinggame.chat.repository.DictionaryWordRepository;
import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.chat.repository.LeaderboardEntryRepository;
import com.typinggame.chat.repository.SystemDictionaryRepository;
import com.typinggame.chat.repository.SystemDictionaryWordRepository;
import com.typinggame.chat.repository.SystemNotificationRepository;
import com.typinggame.chat.repository.UserConversationRepository;
import com.typinggame.chat.service.InternalAdminService;
import com.typinggame.common.admin.AdminPageSupport;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InternalAdminServiceImpl implements InternalAdminService {
    private final SystemDictionaryRepository systemDictionaryRepository;
    private final CustomDictionaryRepository customDictionaryRepository;
    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final UserConversationRepository userConversationRepository;
    private final FriendChatMessageRepository friendChatMessageRepository;
    private final SystemDictionaryWordRepository systemDictionaryWordRepository;
    private final DictionaryWordRepository dictionaryWordRepository;
    private final SystemNotificationRepository systemNotificationRepository;

    public InternalAdminServiceImpl(SystemDictionaryRepository systemDictionaryRepository,
                                    CustomDictionaryRepository customDictionaryRepository,
                                    LeaderboardEntryRepository leaderboardEntryRepository,
                                    UserConversationRepository userConversationRepository,
                                    FriendChatMessageRepository friendChatMessageRepository,
                                    SystemDictionaryWordRepository systemDictionaryWordRepository,
                                    DictionaryWordRepository dictionaryWordRepository,
                                    SystemNotificationRepository systemNotificationRepository) {
        this.systemDictionaryRepository = systemDictionaryRepository;
        this.customDictionaryRepository = customDictionaryRepository;
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.userConversationRepository = userConversationRepository;
        this.friendChatMessageRepository = friendChatMessageRepository;
        this.systemDictionaryWordRepository = systemDictionaryWordRepository;
        this.dictionaryWordRepository = dictionaryWordRepository;
        this.systemNotificationRepository = systemNotificationRepository;
    }

    @Override public Map<String, Object> getSystemDictionaries(int page, int pageSize, String name, String category) {
        String nameFilter = name == null || name.isBlank() ? null : name.trim();
        String categoryFilter = category == null || category.isBlank() ? null : category.trim();
        return AdminPageSupport.pageMap("items", systemDictionaryRepository.search(nameFilter, categoryFilter, pageable(page, pageSize)));
    }
    @Override public Map<String, Object> getCustomDictionaries(int page, int pageSize) { return AdminPageSupport.pageMap("items", customDictionaryRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getLeaderboardEntries(int page, int pageSize) { return AdminPageSupport.pageMap("items", leaderboardEntryRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getUserConversations(int page, int pageSize) { return AdminPageSupport.pageMap("items", userConversationRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getFriendChatMessages(int page, int pageSize) { return AdminPageSupport.pageMap("items", friendChatMessageRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getSystemNotifications(int page, int pageSize) { return AdminPageSupport.pageMap("items", systemNotificationRepository.findAll(pageable(page, pageSize))); }
    @Override public Map<String, Object> getSystemDictionaryWords(Integer dictId, int page, int pageSize) { return AdminPageSupport.pageMap("items", systemDictionaryWordRepository.findByDictId(dictId, pageable(page, pageSize))); }
    @Override public Map<String, Object> getCustomDictionaryWords(Integer dictId, int page, int pageSize) { return AdminPageSupport.pageMap("items", dictionaryWordRepository.findByDictId(dictId, pageable(page, pageSize))); }

    private PageRequest pageable(int page, int pageSize) { return PageRequest.of(Math.max(0, page - 1), Math.max(1, pageSize)); }
}
