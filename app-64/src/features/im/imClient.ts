import { backendBaseUrl } from '@/db/client';
import { issueWsTicket } from '@/services/social';

export type ImEvent = {
  event: string;
  userId?: number;
  receiverId?: number;
  count?: number;
  typing?: boolean;
  online?: boolean;
  clientMsgId?: string;
  message?: string;
};

const listeners = new Set<(event: ImEvent) => void>();
let socket: WebSocket | null = null;
const pendingAck = new Map<string, string>();

export function subscribeIm(listener: (event: ImEvent) => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export async function connectIm() {
  if (socket && socket.readyState <= WebSocket.OPEN) return;
  const ticket = await issueWsTicket();
  const explicit = import.meta.env.VITE_IM_WS_URL as string | undefined;
  const url = explicit
    ? `${explicit}${explicit.includes('?') ? '&' : '?'}ticket=${encodeURIComponent(ticket)}`
    : `${backendBaseUrl.replace(/^http/, 'ws')}/ws?ticket=${encodeURIComponent(ticket)}`;
  socket = new WebSocket(url);
  socket.onmessage = (message) => {
    const event = JSON.parse(message.data) as ImEvent;
    if (event.clientMsgId) pendingAck.delete(event.clientMsgId);
    listeners.forEach((listener) => listener(event));
    if (event.event === 'message' && event.clientMsgId) {
      socket?.send(JSON.stringify({ action: 'ack', clientMsgId: event.clientMsgId }));
    }
  };
}

export function sendChat(receiverId: number, content: string, clientMsgId: string) {
  const payload = JSON.stringify({ action: 'chat', receiverId, content, clientMsgId, type: 'text' });
  pendingAck.set(clientMsgId, payload);
  socket?.send(payload);
  window.setTimeout(() => {
    const again = pendingAck.get(clientMsgId);
    if (again && socket?.readyState === WebSocket.OPEN) socket.send(again);
  }, 3000);
}

export function sendTyping(receiverId: number, typing: boolean) {
  if (socket?.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({ action: 'typing', receiverId, typing }));
  }
}
