package com.typinggame.chat.service;

import java.util.Map;

public interface InternalAdminService {
    Map<String, Object> getSystemDictionaries(int page, int pageSize, String name, String category);
    Map<String, Object> getCustomDictionaries(int page, int pageSize);
    Map<String, Object> getLeaderboardEntries(int page, int pageSize);
    Map<String, Object> getUserConversations(int page, int pageSize);
    Map<String, Object> getFriendChatMessages(int page, int pageSize);
    Map<String, Object> getSystemNotifications(int page, int pageSize);
    Map<String, Object> getSystemDictionaryWords(Integer dictId, int page, int pageSize);
    Map<String, Object> getCustomDictionaryWords(Integer dictId, int page, int pageSize);
}
