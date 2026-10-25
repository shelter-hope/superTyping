import type { MessageDTO } from '@/types/database';
import { IM_RECALL_WINDOW_MS } from './contracts';

export function byTime(a: MessageDTO, b: MessageDTO) {
  return new Date(a.sendTime).getTime() - new Date(b.sendTime).getTime();
}

/** 按 clientMsgId / id 合并消息列表，保持时间序。 */
export function mergeMessages(current: MessageDTO[], incoming: MessageDTO[]): MessageDTO[] {
  const next = [...current];
  for (const message of incoming) {
    const byClient = message.clientMsgId
      ? next.findIndex((item) => item.clientMsgId && item.clientMsgId === message.clientMsgId)
      : -1;
    if (byClient >= 0) {
      next[byClient] = { ...next[byClient], ...message };
      continue;
    }
    const byId = next.findIndex((item) => item.id === message.id);
    if (byId >= 0) next[byId] = { ...next[byId], ...message };
    else next.push(message);
  }
  return next.sort(byTime);
}

export function canRecall(message: MessageDTO, userId: number) {
  if (message.senderId !== userId || message.recalled || message.id < 0) return false;
  return Date.now() - new Date(message.sendTime).getTime() < IM_RECALL_WINDOW_MS;
}
