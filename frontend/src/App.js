import React from 'react';
import {ToastContainer,toast} from 'react-toastify';  // 토스트 라이브러리 추가
import 'react-toastify/dist/ReactToastify.css';

import ThreatTable from './components/ThreatTable';
import BlacklistTable from './components/BlacklistTable';
import LogTable from './components/LogTable';
import useSecurityData from './hooks/useSecurityData';
import useWebSocket from './hooks/useWebSocket';

function App() {
 
  const { logs, threats, blacklists, setLogs,setThreats,setBlacklists ,fetchAllData } = useSecurityData();

  const {isNewThreat} = useWebSocket ((message) => {
    fetchAllData();
  });

 return (
    <div style={{ padding: '30px', backgroundColor: '#f4f7f6', minHeight: '100vh', fontFamily: 'sans-serif' }}>
      <h1>🛡️ 대시보드 </h1>
      {/* 토스트 컨테이너 (팝업이 뜰 위치) */}
      <ToastContainer />

      <button onClick={fetchAllData} style={{ padding: '20px 20px', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer', marginBottom: '20px', fontWeight: 'bold' }}>
        🔄 전체 데이터 새로고침
      </button>
 
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
      <ThreatTable threats={threats} isNewThreat={isNewThreat} />
      <BlacklistTable blacklists={blacklists} />
    </div>
    <LogTable logs={logs} />
  </div>    
  );
}

export default App;