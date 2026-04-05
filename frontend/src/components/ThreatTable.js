import React from 'react';

function ThreatTable ( {threats, isNewThreat}){
return (
        <section style={{ 
          backgroundColor: 'white', padding: '15px', borderRadius: '10px',
           boxShadow: '0 2px 5px rgba(0,0,0,0.1)',
           border: isNewThreat ? '2px solid red' : '1px solid transparent', //테두리 강조
           transition: 'all 0.3s ease' }}>
          <h2 style={{ color: '#d9534f' }}>
            🚨 실시간 위협 탐지 ({threats.length})
            {isNewThreat && <span style={{ marginLeft: '10px', fontSize: '14px', animation: 'blink 0.5s infinite' }}>● NEW</span>}
            </h2>
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
)
}

export default ThreatTable;