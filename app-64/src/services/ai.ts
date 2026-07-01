import { apiClient } from '@/db/client';

export interface AiTrainingSummary {
  contextForPrompt: string;
  focusChars: string[];
  suggestedMode: string;
  recentAccuracy?: number;
  recentSpeed?: number;
  errorComparisonSummary?: string;
}

export async function getAiTrainingSummary(): Promise<AiTrainingSummary> {
  const response = await apiClient.get('/ai/training-summary');
  return response.data.data;
}

export interface AiChatResponse {
  reply: string;
  conversationId: number;
  messageId?: number;
}

export async function postAiChat(message: string, conversationId?: number): Promise<AiChatResponse> {
  const response = await apiClient.post('/ai/chat', { message, conversationId });
  return response.data.data;
}