import React from 'react';

function BlacklistTable({ blacklists,pageInfo,onPageChange }) {
  return (
    <section style={{ backgroundColor: 'white', padding: '15px', borderRadius: '10px', boxShadow: '0 2px 5px rgba(0,0,0,0.1)' }}>
      <h2 style={{ color: '#333' }}>🚫 현재 차단된 IP ({pageInfo.totalElements})</h2>
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
           {/* 페이지 버튼 */}
      <div style = {{
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        gap: "12px",
        marginTop:"16px",
      }}>
        <button
          type = "button"
          title = "이전 페이지"
          aria-label='이전 페이지'
          disabled= {pageInfo.number === 0}
          onClick={() =>
            onPageChange(pageInfo.number-1)
          }
          style={{width: "36px",height:"36px"}}
        >
          {"<"}
        </button>

          <span>
            {pageInfo.totalPages === 0
              ? 0
              : pageInfo.number +1}
            {" / "}
            {pageInfo.totalPages}
          </span>

          <button
            type = "button"
            title='다음 페이지'
            aria-label='다음 페이지'
            disabled={
              pageInfo.number+1 >= pageInfo.totalPages
             }
             onClick={ () =>
                onPageChange(pageInfo.number+1)
             }
             style={{width: "36px",height:"36px"}}
          >
            {">"}
          </button>
             
          <span>전체 {pageInfo.totalElements}건</span>
      </div>

    </section>
  );
}

export default BlacklistTable;