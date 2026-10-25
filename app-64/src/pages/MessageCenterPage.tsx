/**
 * 消息中心页面
 * 
 * @description 显示系统消息、好友消息和好友请求
 * 
 * @features
 * - 系统消息列表
 * - 好友消息列表（会话列表）
 * - 好友请求通知
 * - 未读消息提示
 * - 跳转到聊天界面
 */

import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Bell, MessageCircle, UserPlus, Check, X, Loader2 } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { useImSocket } from '@/hooks/use-im-socket';
import type { MessageDTO, FriendRequestDTO } from '@/types/database';
import { 
  getSystemMessages, 
  getConversations, 
  getFriendRequests, 
  handleFriendRequest, 
  markMessageAsRead,
  markNotificationAsRead,
} from '@/db/api';

/** 会话列表项：后端 getConversations 返回 MessageDTO[]，其中 senderId/senderName 为对方用户 */
export default function MessageCenterPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { toast } = useToast();
  const [systemMessages, setSystemMessages] = useState<MessageDTO[]>([]);
  const [conversations, setConversations] = useState<MessageDTO[]>([]);
  const [friendRequests, setFriendRequests] = useState<FriendRequestDTO[]>([]);
  const [loading, setLoading] = useState(true);

  // 进入消息中心或从聊天返回时拉取数据，保证未读数是最新的
  useEffect(() => {
    const onMessagesPage = location.pathname === '/messages' || location.pathname === '/message-center';
    if (user && onMessagesPage) {
      loadData();
    }
  }, [user, location.pathname]);

  useImSocket((event) => {
    if (event.type === 'MESSAGE' || event.type === 'RECALL') loadConversations();
    if (event.type === 'FRIEND_REQUEST') loadFriendRequests();
  }, !!user);

  async function loadData() {
    setLoading(true);
    try {
      await Promise.all([
        loadSystemMessages(),
        loadConversations(),
        loadFriendRequests(),
      ]);
    } catch (error) {
      console.error('加载数据失败:', error);
    } finally {
      setLoading(false);
    }
  }

  async function loadSystemMessages() {
    try {
      const messages = await getSystemMessages();
      setSystemMessages(messages);
    } catch (error) {
      console.error('Failed to load system messages', error);
    }
  }

  async function loadConversations() {
    try {
      const data = await getConversations();
      setConversations(data);
    } catch (error) {
      console.error('Failed to load conversations', error);
    }
  }

  async function loadFriendRequests() {
    try {
      const requests = await getFriendRequests();
      setFriendRequests(requests);
    } catch (error) {
      console.error('Failed to load friend requests', error);
    }
  }

  async function onAcceptRequest(requestId: number, fromUserId: number) {
    try {
      await handleFriendRequest(requestId, 'accepted');
      toast({
        title: '已接受好友请求',
        description: '你们现在是好友了',
      });
      loadData();
    } catch (error) {
      console.error('接受好友请求失败:', error);
      toast({
        title: '操作失败',
        description: '无法接受好友请求',
        variant: 'destructive',
      });
    }
  }

  async function onRejectRequest(requestId: number) {
    try {
      await handleFriendRequest(requestId, 'rejected');
      toast({
        title: '已拒绝好友请求',
      });
      loadData();
    } catch (error) {
      console.error('拒绝好友请求失败:', error);
      toast({
        title: '操作失败',
        description: '无法拒绝好友请求',
        variant: 'destructive',
      });
    }
  }

  async function onMarkAsRead(message: MessageDTO) {
    try {
      if (message.type === 'notification') {
        await markNotificationAsRead(message.id);
      } else {
        await markMessageAsRead(message.id);
      }
      loadSystemMessages();
    } catch (error) {
      console.error('标记已读失败:', error);
    }
  }

  function handleOpenChat(friendId: number) {
    navigate(`/chat/${friendId}`);
  }

  const unreadSystemCount = systemMessages.filter(m => !m.isRead).length;
  const unreadConversationCount = conversations.reduce((sum, c) => sum + (c.unreadCount ?? 0), 0);
  const pendingRequestCount = friendRequests.length;

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="w-8 h-8 animate-spin" />
      </div>
    );
  }

  return (
    <div className="container max-w-4xl py-8">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Bell className="w-5 h-5" />
            消息中心
          </CardTitle>
          <CardDescription>
            查看系统消息、好友消息和好友请求
          </CardDescription>
        </CardHeader>

        <CardContent>
          <Tabs defaultValue="conversations" className="w-full">
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="conversations" className="relative">
                <MessageCircle className="w-4 h-4 mr-2" />
                好友消息
                {unreadConversationCount > 0 && (
                  <Badge variant="destructive" className="ml-2 h-5 min-w-5 px-1">
                    {unreadConversationCount}
                  </Badge>
                )}
              </TabsTrigger>
              <TabsTrigger value="requests" className="relative">
                <UserPlus className="w-4 h-4 mr-2" />
                好友请求
                {pendingRequestCount > 0 && (
                  <Badge variant="destructive" className="ml-2 h-5 min-w-5 px-1">
                    {pendingRequestCount}
                  </Badge>
                )}
              </TabsTrigger>
              <TabsTrigger value="system" className="relative">
                <Bell className="w-4 h-4 mr-2" />
                系统消息
                {unreadSystemCount > 0 && (
                  <Badge variant="destructive" className="ml-2 h-5 min-w-5 px-1">
                    {unreadSystemCount}
                  </Badge>
                )}
              </TabsTrigger>
            </TabsList>

            {/* 好友消息 */}
            <TabsContent value="conversations">
              <ScrollArea className="h-[500px]">
                {conversations.length === 0 ? (
                  <div className="text-center text-muted-foreground py-12">
                    <MessageCircle className="w-12 h-12 mx-auto mb-4 opacity-50" />
                    <p>暂无好友消息</p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {conversations.map((conv) => (
                      <div
                        key={conv.conversationId ?? conv.id ?? conv.senderId}
                        onClick={() => handleOpenChat(conv.senderId)}
                        className="flex items-center gap-3 p-4 rounded-lg hover:bg-muted cursor-pointer transition-colors"
                      >
                        <Avatar className="w-12 h-12">
                          <AvatarFallback>
                            {conv.senderName?.[0]?.toUpperCase() || '?'}
                          </AvatarFallback>
                        </Avatar>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between mb-1">
                            <span className="font-medium">{conv.senderName ?? '未知'}</span>
                            {conv.sendTime && (
                              <span className="text-xs text-muted-foreground">
                                {new Date(conv.sendTime).toLocaleString('zh-CN', {
                                  month: 'numeric',
                                  day: 'numeric',
                                  hour: '2-digit',
                                  minute: '2-digit',
                                })}
                              </span>
                            )}
                          </div>
                          <div className="flex items-center justify-between">
                            <p className="text-sm text-muted-foreground truncate">
                              {conv.content || '暂无消息'}
                            </p>
                            {(conv.unreadCount ?? 0) > 0 && (
                              <Badge variant="destructive" className="ml-2 h-5 min-w-5 px-1">
                                {conv.unreadCount}
                              </Badge>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </ScrollArea>
            </TabsContent>

            {/* 好友请求 */}
            <TabsContent value="requests">
              <ScrollArea className="h-[500px]">
                {friendRequests.length === 0 ? (
                  <div className="text-center text-muted-foreground py-12">
                    <UserPlus className="w-12 h-12 mx-auto mb-4 opacity-50" />
                    <p>暂无好友请求</p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {friendRequests.map((request) => (
                      <div
                        key={request.id}
                        className="flex items-center gap-3 p-4 rounded-lg border"
                      >
                        <Avatar className="w-12 h-12">
                          <AvatarFallback>
                            {request.senderName?.[0]?.toUpperCase() || '?'}
                          </AvatarFallback>
                        </Avatar>
                        <div className="flex-1 min-w-0">
                          <p className="font-medium">{request.senderName}</p>
                          {request.applyMessage && (
                            <p className="text-sm text-muted-foreground mt-1 truncate">{request.applyMessage}</p>
                          )}
                          <p className="text-xs text-muted-foreground mt-1">
                            {new Date(request.sendTime).toLocaleString('zh-CN')}
                          </p>
                        </div>
                        <div className="flex gap-2">
                          <Button
                            size="sm"
                            onClick={() => onAcceptRequest(request.id, request.senderId)}
                          >
                            <Check className="w-4 h-4 mr-1" />
                            接受
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => onRejectRequest(request.id)}
                          >
                            <X className="w-4 h-4 mr-1" />
                            拒绝
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </ScrollArea>
            </TabsContent>

            {/* 系统消息 */}
            <TabsContent value="system">
              <ScrollArea className="h-[500px]">
                {systemMessages.length === 0 ? (
                  <div className="text-center text-muted-foreground py-12">
                    <Bell className="w-12 h-12 mx-auto mb-4 opacity-50" />
                    <p>暂无系统消息</p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {systemMessages.map((message) => (
                      <div
                        key={`${message.type}-${message.id}`}
                        className={`p-4 rounded-lg border ${
                          message.isRead ? 'bg-muted/30' : 'bg-primary/5 border-primary/20'
                        }`}
                      >
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <p className="text-sm">{message.content}</p>
                            <p className="text-xs text-muted-foreground mt-2">
                              {new Date(message.sendTime).toLocaleString('zh-CN')}
                            </p>
                          </div>
                          {!message.isRead && (
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => onMarkAsRead(message)}
                            >
                              标记已读
                            </Button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </ScrollArea>
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  );
}
