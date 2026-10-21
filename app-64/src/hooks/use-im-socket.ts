import { useEffect, useRef, useState } from 'react';
import { imSocket, type ImEvent, type ImStatus } from '@/services/imSocket';

/**
 * 在组件生命周期内保持 IM 长连接，并订阅推送事件。
 * enabled 为 false（例如未登录）时不建立连接。
 */
export function useImSocket(onEvent: (event: ImEvent) => void, enabled = true): ImStatus {
  const [status, setStatus] = useState<ImStatus>(imSocket.getStatus());
  const handlerRef = useRef(onEvent);
  handlerRef.current = onEvent;

  useEffect(() => {
    if (!enabled) return;
    const offStatus = imSocket.onStatus(setStatus);
    const offEvent = imSocket.onEvent((e) => handlerRef.current(e));
    imSocket.acquire();
    setStatus(imSocket.getStatus());
    return () => {
      offEvent();
      offStatus();
      imSocket.release();
    };
  }, [enabled]);

  return status;
}
