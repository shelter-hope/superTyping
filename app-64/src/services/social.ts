import { apiClient } from '@/db/client';
import type { FriendResponseDTO, FriendRequestDTO, MessageDTO } from '@/types/database';

export async function getFriendList(): Promise<FriendResponseDTO[]> {
  const response = await apiClient.get('/friends');
  return response.data.data || [];
}

export async function searchUsers(keyword: string): Promise<FriendResponseDTO[]> {
  const response = await apiClient.get('/friends/search', {
    params: { keyword }
  });
  return response.data.data || [];
}

export async function sendFriendRequest(receiverId: number): Promise<FriendRequestDTO> {
  const response = await apiClient.post('/friends/request', null, {
    params: { receiverId }
  });
  return response.data.data;
}

export async function handleFriendRequest(requestId: number, status: 'accepted' | 'rejected'): Promise<boolean> {
  const response = await apiClient.patch(`/friends/request/${requestId}`, null, {
    params: { status }
  });
  return response.data.data;
}

export async function getFriendRequests(): Promise<FriendRequestDTO[]> {
  const response = await apiClient.get('/friends/request');
  return response.data.data || [];
}

export async function deleteFriend(friendId: number): Promise<boolean> {
  const response = await apiClient.delete(`/friends/${friendId}`);
  return response.data.data;
}

export async function getConversations(): Promise<MessageDTO[]> {
  const response = await apiClient.get('/messages/conversations');
  return response.data.data || [];
}

export async function getConversationMessages(conversationId: number, page = 0, size = 20): Promise<MessageDTO[]> {
  const response = await apiClient.get(`/messages/conversation/${conversationId}`, {
    params: { page, size }
  });
  return response.data.data || [];
}

export async function getMessagesByFriend(friendId: number, offset = 0): Promise<MessageDTO[]> {
  const response = await apiClient.get('/messages', {
    params: { friendId, offset }
  });
  return response.data.data || [];
}

export async function sendMessage(receiverId: number, content: string, type = 'text'): Promise<MessageDTO> {
  const response = await apiClient.post('/messages', null, {
    params: { receiverId, content, type }
  });
  return response.data.data;
}

export async function markMessageAsRead(messageId: number): Promise<boolean> {
  const response = await apiClient.patch(`/messages/${messageId}/read`);
  return response.data.data;
}

export async function markAsReadByFriend(friendId: number): Promise<boolean> {
  const response = await apiClient.post('/messages/read-by-friend', null, {
    params: { friendId },
  });
  return response.data.data;
}

export async function getUnreadCount(): Promise<number> {
  const response = await apiClient.get('/messages/unread-count');
  return response.data.data;
}

export async function getSystemMessages(): Promise<MessageDTO[]> {
  const response = await apiClient.get('/messages/system');
  return response.data.data || [];
}