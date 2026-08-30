import React, {useState} from 'react';
import {ToastContainer} from 'react-toastify';  
import 'react-toastify/dist/ReactToastify.css';

import ThreatTable from './components/ThreatTable';
import BlacklistTable from './components/BlacklistTable';
import LogTable from './components/LogTable';
import useSecurityData from './hooks/useSecurityData';
import useWebSocket from './hooks/useWebSocket';
import LogDetailModal from './components/LogDetailModal';

function App() {
 
  const { 
      logs, 
      threats,
      blacklists, 
      logPage,
      threatPage,
      blacklistPage,
      fetchLogs,
      fetchLogById,
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

  const [isLogModalOpen, setIsLogModalOpen] = useState(false);
  const [selectedLog,setSelectedLog] = useState(null);
  const [isLogLoading,setIsLogLoading] = useState(false);
  const [logDetailError,setLogDetailError] = useState('');

  const handleViewLog = async logId => {
  setIsLogModalOpen(true);
  setSelectedLog(null);
  setLogDetailError('');
  setIsLogLoading(true);

  try {
    const log = await fetchLogById(logId);
    setSelectedLog(log);
  } catch (error) {
    console.error('로그 상세 조회 실패', error);

    if (error.response?.status === 404) {
      setLogDetailError('해당 원본 로그를 찾을 수 없습니다.');
    } else {
      setLogDetailError('로그 상세 정보를 불러오지 못했습니다.');
    }
  } finally {
    setIsLogLoading(false);
  }
};

const handleCloseLogModal = () => {
  setIsLogModalOpen(false);
  setSelectedLog(null);
  setLogDetailError('');
};

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

      <LogDetailModal
        isOpen = {isLogModalOpen}
        log = {selectedLog}
        loading={isLogLoading}
        error = {logDetailError}
        onClose={handleCloseLogModal}
      />

      <button onClick={fetchAllData} style={{ padding: '20px 20px', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer', marginBottom: '20px', fontWeight: 'bold' }}>
        🔄 전체 데이터 새로고침
      </button>
      <LogTable
      logs={logs}
      pageInfo = {logPage}
      onPageChange = {fetchLogs}
      onSearch={searchLogs}
      onReset={resetLogSearch}
     />
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px',marginTop:"20px", }}>
      <ThreatTable 
        threats={threats}
        isNewThreat={isNewThreat}
        pageInfo= {threatPage}
        onPageChange = {fetchThreats}
        onSearch = {searchThreats}
        onReset={resetThreatSearch}
        onViewLog={handleViewLog}
       />
      <BlacklistTable 
        blacklists={blacklists} 
        pageInfo= {blacklistPage}
        onPageChange = {fetchBlacklists}
        onSearch={searchBlacklists}
        onReset={resetBlacklistSearch}
        onViewLog={handleViewLog}
      />
    </div>
  </div>    
  );
}

export default App;