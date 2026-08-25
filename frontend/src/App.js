import React, {useState} from 'react';
import {ToastContainer} from 'react-toastify';  // 토스트 라이브러리 추가
import 'react-toastify/dist/ReactToastify.css';

import ThreatTable from './components/ThreatTable';
import BlacklistTable from './components/BlacklistTable';
import LogTable from './components/LogTable';
import LogDetailModal from './components/LogDetailModal';
import useSecurityData from './hooks/useSecurityData';
import useWebSocket from './hooks/useWebSocket';

function App() {
  // 전체 로그와 위협 목록이 동일한 상세 모달을 공유한다.
  const [selectedLogId, setSelectedLogId] = useState(null);
 
  const { 
      logs, 
      threats,
      blacklists, 
      logPage,
      threatPage,
      blacklistPage,
      fetchLogs,
      fetchThreats,
      fetchBlacklists,
      fetchAllData, 
      searchLogs,
      resetLogSearch,
      searchThreats,
      resetThreatSearch,
      searchBlacklists,
      resetBlacklistSearch
  } = useSecurityData();

  const {isNewThreat, connectionStatus} 
    = useWebSocket (() => {
    fetchThreats(0);
    fetchBlacklists(0);
  });

 return (
    <div style={{ padding: '30px', backgroundColor: '#f4f7f6', minHeight: '100vh', fontFamily: 'sans-serif' }}>
      <h1>🛡️대시보드</h1>
      {/* 토스트 컨테이너 (팝업이 뜰 위치) */}
      <ToastContainer />
      {connectionStatus !== "connected" && (
        <div role= "status">
          {connectionStatus === "connecting"
            ? "실시간 알림 연결 중..."
            : connectionStatus === "reconnecting"
            ? "실시간 알림 재연결 중..."
            : "실시간 알림 연결 오류"
          }
           </div>
      )}

      <button onClick={fetchAllData} style={{ padding: '20px 20px', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer', marginBottom: '20px', fontWeight: 'bold' }}>
        🔄 전체 데이터 새로고침
      </button>
      <LogTable
      logs={logs}
      pageInfo = {logPage}
      onPageChange = {fetchLogs}
      onSearch={searchLogs}
      onReset={resetLogSearch}
      onLogSelect={setSelectedLogId}
     />
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px',marginTop:"20px", }}>
      <ThreatTable 
        threats={threats}
        isNewThreat={isNewThreat}
        pageInfo= {threatPage}
        onPageChange = {fetchThreats}
        onSearch = {searchThreats}
        onReset={resetThreatSearch}
        onLogSelect={setSelectedLogId}
       />
      <BlacklistTable 
        blacklists={blacklists} 
        pageInfo= {blacklistPage}
        onPageChange = {fetchBlacklists}
        onSearch={searchBlacklists}
        onReset={resetBlacklistSearch}
      />
    </div>
    <LogDetailModal
      logId={selectedLogId}
      onClose={() => setSelectedLogId(null)}
    />
  </div>    
  );
}

export default App;
