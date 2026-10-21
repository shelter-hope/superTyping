import { backendBaseUrl } from '@/db/client';
import type { MessageDTO } from '@/types/database';

export type ImStatus = 'idle' | 'connecting' | 'open' | 'closed';

export type ImEvent =
  | { type: 'MESSAGE'; message: MessageDTO }
  | { type: 'READ'; readerId: number }
  | { type: 'PONG' };

type EventListener = (event: ImEvent) => void;
type StatusListener = (status: ImStatus) => void;

const HEARTBEAT_MS = 25_000;
const RECONNECT_BASE_MS = 1_000;
const RECONNECT_MAX_MS = 15_000;

function buildUrl(token: string): string {
  const base =
    (import.meta.env.VITE_IM_WS_URL as string | undefined) ||
    backendBaseUrl.replace(/^http/, 'ws') + '/ws/im';
  return `${base}?token=${encodeURIComponent(token)}`;
}

/**
 * 全局唯一的 IM 长连接。页面通过 acquire/release 引用计数共享，
 * 最后一个使用者 release 后才真正断开。
 */
class ImSocket {
  private ws: WebSocket | null = null;
  private status: ImStatus = 'idle';
  private refCount = 0;
  private retry = 0;
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly eventListeners = new Set<EventListener>();
  private readonly statusListeners = new Set<StatusListener>();

  acquire() {
    this.refCount += 1;
    if (this.refCount === 1) this.connect();
  }

  release() {
    this.refCount = Math.max(0, this.refCount - 1);
    if (this.refCount === 0) this.shutdown();
  }

  getStatus(): ImStatus {
    return this.status;
  }

  onEvent(listener: EventListener): () => void {
    this.eventListeners.add(listener);
    return () => this.eventListeners.delete(listener);
  }

  onStatus(listener: StatusListener): () => void {
    this.statusListeners.add(listener);
    return () => this.statusListeners.delete(listener);
  }

  private setStatus(status: ImStatus) {
    if (this.status === status) return;
    this.status = status;
    this.statusListeners.forEach((l) => l(status));
  }

  private connect() {
    // token 可能在重连期间刷新，每次连接都重新读取
    const token = localStorage.getItem('auth_token');
    if (!token) {
      this.setStatus('closed');
      return;
    }

    this.setStatus('connecting');
    const ws = new WebSocket(buildUrl(token));
    this.ws = ws;

    ws.onopen = () => {
      if (this.ws !== ws) return;
      this.retry = 0;
      this.setStatus('open');
      this.startHeartbeat();
    };

    ws.onmessage = (e) => {
      if (this.ws !== ws) return;
      let event: ImEvent;
      try {
        event = JSON.parse(e.data as string);
      } catch {
        return;
      }
      this.eventListeners.forEach((l) => l(event));
    };

    ws.onclose = () => {
      if (this.ws !== ws) return;
      this.ws = null;
      this.stopHeartbeat();
      this.setStatus('closed');
      this.scheduleReconnect();
    };

    ws.onerror = () => {
      // onclose 随后触发，统一在那里处理重连
    };
  }

  private scheduleReconnect() {
    if (this.refCount === 0 || this.reconnectTimer) return;
    const delay = Math.min(RECONNECT_BASE_MS * 2 ** this.retry, RECONNECT_MAX_MS);
    this.retry += 1;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      if (this.refCount > 0) this.connect();
    }, delay);
  }

  private startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeatTimer = setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ type: 'PING' }));
      }
    }, HEARTBEAT_MS);
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  private shutdown() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.stopHeartbeat();
    const ws = this.ws;
    this.ws = null;
    ws?.close();
    this.retry = 0;
    this.setStatus('idle');
  }
}

export const imSocket = new ImSocket();
