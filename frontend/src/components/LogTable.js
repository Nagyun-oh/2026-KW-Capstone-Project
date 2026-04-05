import React from 'react';

function LogTable({ logs }) {
  return (
    <section style={{ marginTop: '30px', backgroundColor: 'white', padding: '15px', borderRadius: '10px', boxShadow: '0 2px 5px rgba(0,0,0,0.1)' }}>
      <h2 style={{ color: '#007bff' }}>📑 전체 네트워크 로그 ({logs.length})</h2>
      <table border="1" style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead style={{ backgroundColor: '#eef6ff' }}>
          <tr>
            <th>IP</th>
            <th>Method</th>
            <th>URL</th>
            <th>Status</th>
            <th>Time</th>
          </tr>
        </thead>
        <tbody>
          {logs.map(log => (
            <tr key={log.id} style={{ textAlign: 'center' }}>
              <td>{log.ipAddress}</td>
              <td>{log.requestMethod}</td>
              <td>{log.requestUrl}</td>
              <td style={{ color: log.statusCode === 403 ? 'red' : 'black' }}>{log.statusCode}</td>
              <td>{new Date(log.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export default LogTable;