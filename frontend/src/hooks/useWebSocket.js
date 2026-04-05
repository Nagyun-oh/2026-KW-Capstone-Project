import { useState, useEffect } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import { toast } from 'react-toastify';

function useWebSocket(onThreatReceived) {
  const [isNewThreat, setIsNewThreat] = useState(false);

  useEffect(() => {
    const socket = new SockJS(process.env.REACT_APP_WS_URL);
    const stompClient = Stomp.over(socket);

    stompClient.connect({}, (frame) => {
      console.log('Connected: ' + frame);

      stompClient.subscribe('/topic/threats', (sdkEvent) => {
        const message = JSON.parse(sdkEvent.body);
        const toastId = `threat-${message.ip} - ${message.time}`;

        console.log("🚨 실시간 위협 알림 수신!", message);

        if (!toast.isActive(toastId)) {
          toast.error(`위협 감지! [${message.type}] IP: ${message.ip}`, {
            toastId: toastId,
            position: "top-right",
            autoClose: 5000,
          });
        }

        onThreatReceived(message);

        setIsNewThreat(true);
        setTimeout(() => setIsNewThreat(false), 2000);
      });
    });

    return () => {
      if (stompClient && stompClient.connected) {
        console.log("경고: 컴포넌트 언마운트로 인해 소켓 연결을 종료합니다.");
        stompClient.disconnect();
      }
    };
  }, []);

  return { isNewThreat };
}

export default useWebSocket;