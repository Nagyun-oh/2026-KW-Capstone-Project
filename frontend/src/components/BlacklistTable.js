import React from 'react';

function BlacklistTable({ blacklists }) {
  return (
    <section style={{ backgroundColor: 'white', padding: '15px', borderRadius: '10px', boxShadow: '0 2px 5px rgba(0,0,0,0.1)' }}>
      <h2 style={{ color: '#333' }}>🚫 현재 차단된 IP ({blacklists.length})</h2>
      <table border="1" style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead style={{ backgroundColor: '#f8f9fa' }}>
          <tr>
            <th>IP 주소</th>
            <th>차단 사유</th>
            <th>위험 수치</th>
          </tr>
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
  );
}

export default BlacklistTable;