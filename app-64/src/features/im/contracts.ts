/** 与后端 ImContracts / MessagePolicy 对齐的即时通信契约。 */

export const IM_RECALL_MINUTES = 2;
export const IM_RECALL_WINDOW_MS = IM_RECALL_MINUTES * 60 * 1000;

export const IM_EVENT = {
  MESSAGE: 'MESSAGE',
  READ: 'READ',
  RECALL: 'RECALL',
  UNREAD: 'UNREAD',
  TYPING: 'TYPING',
  PRESENCE: 'PRESENCE',
  FRIEND_REQUEST: 'FRIEND_REQUEST',
  ACK: 'ACK',
  ERROR: 'ERROR',
  PING: 'PING',
  PONG: 'PONG',
} as const;

export const IM_ACTION = {
  chat: 'chat',
  typing: 'typing',
  ack: 'ack',
} as const;
