import { useCallback, useEffect, useState, type RefObject } from 'react';

interface FullscreenDocument extends Document {
  webkitFullscreenElement?: Element | null;
  mozFullScreenElement?: Element | null;
  msFullscreenElement?: Element | null;
  webkitExitFullscreen?: () => Promise<void> | void;
  mozCancelFullScreen?: () => Promise<void> | void;
  msExitFullscreen?: () => Promise<void> | void;
}

interface FullscreenElement extends HTMLElement {
  webkitRequestFullscreen?: () => Promise<void> | void;
  mozRequestFullScreen?: () => Promise<void> | void;
  msRequestFullscreen?: () => Promise<void> | void;
}

export function useFullscreen(targetRef: RefObject<HTMLElement | null>) {
  const [isFullscreen, setIsFullscreen] = useState(false);

  const readFullscreenState = useCallback(() => {
    const doc = document as FullscreenDocument;
    return Boolean(
      doc.fullscreenElement ||
        doc.webkitFullscreenElement ||
        doc.mozFullScreenElement ||
        doc.msFullscreenElement
    );
  }, []);

  const toggleFullscreen = useCallback(async () => {
    const elem = targetRef.current as FullscreenElement | null;
    if (!elem) throw new Error('游戏容器未加载');

    const doc = document as FullscreenDocument;
    if (!readFullscreenState()) {
      if (elem.requestFullscreen) return elem.requestFullscreen();
      if (elem.webkitRequestFullscreen) return elem.webkitRequestFullscreen();
      if (elem.mozRequestFullScreen) return elem.mozRequestFullScreen();
      if (elem.msRequestFullscreen) return elem.msRequestFullscreen();
      throw new Error('浏览器不支持全屏API');
    }

    if (doc.exitFullscreen) return doc.exitFullscreen();
    if (doc.webkitExitFullscreen) return doc.webkitExitFullscreen();
    if (doc.mozCancelFullScreen) return doc.mozCancelFullScreen();
    if (doc.msExitFullscreen) return doc.msExitFullscreen();
  }, [readFullscreenState, targetRef]);

  useEffect(() => {
    const handleChange = () => setIsFullscreen(readFullscreenState());
    document.addEventListener('fullscreenchange', handleChange);
    document.addEventListener('webkitfullscreenchange', handleChange);
    document.addEventListener('mozfullscreenchange', handleChange);
    document.addEventListener('MSFullscreenChange', handleChange);
    handleChange();

    return () => {
      document.removeEventListener('fullscreenchange', handleChange);
      document.removeEventListener('webkitfullscreenchange', handleChange);
      document.removeEventListener('mozfullscreenchange', handleChange);
      document.removeEventListener('MSFullscreenChange', handleChange);
    };
  }, [readFullscreenState]);

  return { isFullscreen, toggleFullscreen };
}
