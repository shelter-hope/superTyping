export const STICKERS = [
  { id: 'smile', label: '微笑' },
  { id: 'wave', label: '挥手' },
  { id: 'gg', label: '好球' },
  { id: 'fire', label: '加油' },
  { id: 'think', label: '思考' },
] as const;

export type StickerId = (typeof STICKERS)[number]['id'];

export function stickerSrc(id: string): string {
  return `/stickers/${id}.svg`;
}

export function isSticker(id: string): boolean {
  return STICKERS.some((sticker) => sticker.id === id);
}
