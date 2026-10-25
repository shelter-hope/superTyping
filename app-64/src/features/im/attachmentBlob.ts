import { useEffect, useState } from 'react';
import { downloadAttachment } from '@/db/api';

/** 加载聊天附件为 object URL，卸载时释放。 */
export function useAttachmentObjectUrl(attachmentId: string) {
  const [url, setUrl] = useState('');
  useEffect(() => {
    let objectUrl = '';
    let cancelled = false;
    downloadAttachment(Number(attachmentId))
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch(() => setUrl(''));
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [attachmentId]);
  return url;
}

export async function downloadAttachmentAsFile(attachmentId: string, fileName: string) {
  const blob = await downloadAttachment(Number(attachmentId));
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName || 'file';
  link.click();
  URL.revokeObjectURL(url);
}
