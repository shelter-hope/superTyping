import { useCallback, useEffect, useRef, useState } from 'react';
import { useToast } from '@/hooks/use-toast';
import { useImSocket } from '@/hooks/use-im-socket';
import { imSocket, type ImEvent, type ImStatus } from '@/services/imSocket';
import type { MessageDTO, Profile } from '@/types/database';
import {
  getProfile,
  getMessagesByFriend,
  sendMessage,
  markAsReadByFriend,
  uploadAttachment,
  recallMessage,
} from '@/db/api';
import { IM_EVENT } from './contracts';
import { mergeMessages } from './messageMerge';

const POLL_INTERVAL_MS = 3000;

export function useChatSession(user: { userId: number; username?: string } | null, friendId: number) {
  const { toast } = useToast();
  const [friend, setFriend] = useState<Profile | null>(null);
  const [messages, setMessages] = useState<MessageDTO[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [friendTyping, setFriendTyping] = useState(false);
  const typingSent = useRef(false);
  const typingStop = useRef<number>();
  const typingHide = useRef<number>();

  const handleImEvent = useCallback(
    (event: ImEvent) => {
      if (!user || !friendId) return;
      if (event.type === IM_EVENT.MESSAGE) {
        const m = event.message;
        const fromFriend = m.senderId === friendId && m.receiverId === user.userId;
        const fromMe = m.senderId === user.userId && m.receiverId === friendId;
        if (!fromFriend && !fromMe) return;
        setMessages((prev) => mergeMessages(prev, [m]));
        if (fromFriend) {
          markAsReadByFriend(friendId).catch((e) => console.error('标记已读失败:', e));
        }
      } else if (event.type === IM_EVENT.ACK) {
        setMessages((prev) =>
          prev.map((m) =>
            m.clientMsgId === event.clientMsgId ? { ...m, id: event.id, serverSeq: event.serverSeq } : m
          )
        );
      } else if (event.type === IM_EVENT.READ && event.readerId === friendId) {
        setMessages((prev) =>
          prev.map((m) => (m.senderId === user.userId && !m.isRead ? { ...m, isRead: true } : m))
        );
      } else if (event.type === IM_EVENT.RECALL) {
        setMessages((prev) =>
          prev.map((m) =>
            m.clientMsgId === event.clientMsgId ? { ...m, recalled: true, content: '' } : m
          )
        );
      } else if (event.type === IM_EVENT.TYPING && event.userId === friendId) {
        setFriendTyping(event.typing);
        window.clearTimeout(typingHide.current);
        if (event.typing) {
          typingHide.current = window.setTimeout(() => setFriendTyping(false), 3000);
        }
      } else if (event.type === IM_EVENT.ERROR) {
        toast({ title: '发送失败', description: event.message, variant: 'destructive' });
        if (event.clientMsgId) {
          setMessages((prev) => prev.filter((m) => m.clientMsgId !== event.clientMsgId || m.id > 0));
        }
      }
    },
    [user, friendId, toast]
  );

  const imStatus = useImSocket(handleImEvent, !!user);

  const loadMessages = useCallback(async () => {
    if (!user || !friendId) return;
    try {
      const msgs = await getMessagesByFriend(friendId, 0);
      setMessages((prev) => mergeMessages(prev.filter((m) => m.id < 0), msgs));
    } catch (error) {
      console.error('Failed to load messages', error);
    }
  }, [user, friendId]);

  useEffect(() => {
    if (!user || !friendId) return;
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const data = await getProfile(friendId);
        if (!cancelled) setFriend(data);
        await markAsReadByFriend(friendId);
        await loadMessages();
      } catch (error) {
        console.error('加载数据失败:', error);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user, friendId, loadMessages]);

  useEffect(() => {
    if (!user || !friendId) return;
    if (imStatus === 'open') {
      loadMessages();
      return;
    }
    const interval = setInterval(loadMessages, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [user, friendId, imStatus, loadMessages]);

  function notifyTyping(value: string) {
    if (imStatus !== 'open' || !friendId) return;
    if (!value) {
      if (typingSent.current) {
        imSocket.send({ action: 'typing', receiverId: friendId, typing: false });
        typingSent.current = false;
      }
      window.clearTimeout(typingStop.current);
      return;
    }
    if (!typingSent.current) {
      imSocket.send({ action: 'typing', receiverId: friendId, typing: true });
      typingSent.current = true;
    }
    window.clearTimeout(typingStop.current);
    typingStop.current = window.setTimeout(() => {
      imSocket.send({ action: 'typing', receiverId: friendId, typing: false });
      typingSent.current = false;
    }, 2000);
  }

  async function deliver(content: string, type: string, extra: Partial<MessageDTO> = {}) {
    if (!user || !friendId) return;
    const clientMsgId = crypto.randomUUID();
    const optimistic: MessageDTO = {
      id: -Date.now(),
      senderId: user.userId,
      senderName: user.username || '我',
      receiverId: friendId,
      content,
      sendTime: new Date().toISOString(),
      isRead: false,
      type,
      clientMsgId,
      ...extra,
    };
    setMessages((prev) => mergeMessages(prev, [optimistic]));
    const sentOnSocket =
      imStatus === 'open' &&
      imSocket.send({
        action: 'chat',
        receiverId: friendId,
        content,
        type,
        clientMsgId,
      });
    if (sentOnSocket) return;
    try {
      const saved = await sendMessage(friendId, content, type, clientMsgId);
      setMessages((prev) => mergeMessages(prev, [saved]));
    } catch (error) {
      setMessages((prev) => prev.filter((m) => m.clientMsgId !== clientMsgId));
      throw error;
    }
  }

  async function handleSend() {
    if (!input.trim() || sending || !friendId) return;
    const content = input.trim();
    setInput('');
    notifyTyping('');
    setSending(true);
    try {
      await deliver(content, 'text');
    } catch (error) {
      console.error('发送消息失败:', error);
      toast({ title: '发送失败', description: '无法发送消息', variant: 'destructive' });
      setInput(content);
    } finally {
      setSending(false);
    }
  }

  async function handleSticker(id: string) {
    setSending(true);
    try {
      await deliver(id, 'emoji');
    } catch (error) {
      toast({
        title: '发送失败',
        description: error instanceof Error ? error.message : '无法发送表情',
        variant: 'destructive',
      });
    } finally {
      setSending(false);
    }
  }

  async function handleFile(file: File) {
    setSending(true);
    try {
      const uploaded = await uploadAttachment(file);
      const type = uploaded.kind === 5 ? 'image' : 'file';
      await deliver(String(uploaded.id), type, {
        attachmentName: uploaded.originalName,
        attachmentMediaType: uploaded.mediaType,
        attachmentSize: uploaded.sizeBytes,
      });
    } catch (error) {
      toast({
        title: '上传失败',
        description: error instanceof Error ? error.message : '无法发送文件',
        variant: 'destructive',
      });
    } finally {
      setSending(false);
    }
  }

  async function handleRecall(message: MessageDTO) {
    try {
      await recallMessage(message.id);
      setMessages((prev) =>
        prev.map((item) =>
          item.id === message.id || (message.clientMsgId && item.clientMsgId === message.clientMsgId)
            ? { ...item, recalled: true, content: '' }
            : item
        )
      );
    } catch (error) {
      toast({
        title: '撤回失败',
        description: error instanceof Error ? error.message : '无法撤回',
        variant: 'destructive',
      });
    }
  }

  return {
    friend,
    messages,
    input,
    setInput,
    loading,
    sending,
    friendTyping,
    imStatus: imStatus as ImStatus,
    notifyTyping,
    handleSend,
    handleSticker,
    handleFile,
    handleRecall,
    toast,
  };
}
