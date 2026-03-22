import React, {useState, useEffect} from 'react';
import axios from 'axios'

function App(){
  const [logs, setLogs] = useState([]);
  const [threats,setThreats] = useState([]);
  const [blacklists,setBlacklists] = useState([]);

  // 모든 데이터를 가져오는 함수
  const fetchAllData = () => {
    // 1. 전체 로그
    axios.get('http://localhost:8080/api/v1/logs')
      .then(response => setLogs(response.data))
      .catch(err => console.error("전체 로그 로딩 실패",err));
    // 2. 위협 로그
    axios.get('http://localhost:8080/api/v1/threats')
      .then(response => setThreats(response.data))
      .catch(err => console.error("위협 로그 로딩 실패",err));
    // 3. 블랙리스트
    axios.get('http://localhost:8080/api/v1/blacklist')
      .then(response => setBlacklists(response.data))
      .catch(err => console.error("블랙리스트 로그 로딩 실패",err));
  };

  useEffect( () =>{
    fetchAllData();
  }, []);

 return (
    <div style={{ padding: '30px', backgroundColor: '#f4f7f6', minHeight: '100vh', fontFamily: 'sans-serif' }}>
      <h1>🛡️ AI 보안 통합 관제 센터</h1>
      
      <button onClick={fetchAllData} style={{ padding: '20px 20px', backgroundColor: '#007bff', color: 'white', border: 'none', borderRadius: '5px', cursor: 'pointer', marginBottom: '20px', fontWeight: 'bold' }}>
        🔄 전체 데이터 새로고침
      </button>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
        {/* --- 섹션 1: 실시간 위협 (왼쪽) --- */}
        <section style={{ backgroundColor: 'white', padding: '15px', borderRadius: '10px', boxShadow: '0 2px 5px rgba(0,0,0,0.1)' }}>
          <h2 style={{ color: '#d9534f' }}>🚨 실시간 위협 탐지 ({threats.length})</h2>
          <table border="1" style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead style={{ backgroundColor: '#fff5f5' }}>
              <tr>
                <th>유형</th>
                <th>위험도</th>
                <th>설명</th>
                </tr>
            </thead>
            <tbody>
              {threats.map(t => (
                <tr key={t.id} style={{ textAlign: 'center' }}>
                  <td>{t.threatType}</td>
                  <td style={{ color: t.severity === 'CRITICAL' ? 'red' : 'orange' }}>{t.severity}</td>
                  <td>{t.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {/* --- 섹션 2: 블랙리스트 명단 (오른쪽) --- */}
        <section style={{ backgroundColor: 'white', padding: '15px', borderRadius: '10px', boxShadow: '0 2px 5px rgba(0,0,0,0.1)' }}>
          <h2 style={{ color: '#333' }}>🚫 현재 차단된 IP ({blacklists.length})</h2>
          <table border="1" style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead style={{ backgroundColor: '#f8f9fa' }}>
              <tr><th>IP 주소</th><th>차단 사유</th><th>위험 수치</th></tr>
            </thead>
            <tbody>
              {blacklists.map(b => (
                <tr key={b.id} style={{ textAlign: 'center' }}>
                  <td style={{ fontWeight: 'bold' }}>{b.ipAddress}</td>
                  <td>{b.reason}</td>
                  <td>{b.dangerLevel}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>

      {/* --- 섹션 3: 전체 로그 (하단) --- */}
      <section style={{ marginTop: '30px', backgroundColor: 'white', padding: '15px', borderRadius: '10px', boxShadow: '0 2px 5px rgba(0,0,0,0.1)' }}>
        <h2 style={{ color: '#007bff' }}>📑 전체 네트워크 로그 ({logs.length})</h2>
        <table border="1" style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead style={{ backgroundColor: '#eef6ff' }}>
            <tr><th>IP</th><th>Method</th><th>URL</th><th>Status</th><th>Time</th></tr>
          </thead>
          <tbody>
            {logs.map(log => (
              <tr key={log.id} style={{ textAlign: 'center' }}>
                <td>{log.ipAddress}</td>
                <td>{log.requestMethod}</td>
                <td>{log.requestUrl}</td>
                <td style={{ color: log.statusCode == 403 ? 'red' : 'black' }}>{log.statusCode}</td>
                <td>{new Date(log.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

export default App;
