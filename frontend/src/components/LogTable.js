import React, {useState} from 'react';

function LogTable({ logs,pageInfo,onPageChange,onSearch,onReset,onLogSelect, }) {
  // 검색 입력창의 현재 값을 관리
  const [form,setForm] = useState({
    ip: "",
    method: "",
    statusCode: "",
  });

  const handleChange = event => {
    const {name,value} = event.target;

    setForm(previous => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleSubmit = event => {
    event.preventDefault();
    onSearch(form);
  };

  const handleReset = () => {
    const emptyForm = {
      ip: "",
      method: "",
      statusCode: "",
    };
    setForm(emptyForm);
    onReset();
  }

  return (
    <section style={{ marginTop: '30px', backgroundColor: 'white', padding: '15px', borderRadius: '10px', boxShadow: '0 2px 5px rgba(0,0,0,0.1)' }}>
      
    <form onSubmit={handleSubmit}>
      <input
        name="ip"
        value={form.ip}
        onChange={handleChange}
        placeholder="IP address"
      />

      <select
        name="method"
        value={form.method}
        onChange={handleChange}
      >
        <option value="">All methods</option>
        <option value="GET">GET</option>
        <option value="POST">POST</option>
        <option value="PUT">PUT</option>
        <option value="PATCH">PATCH</option>
        <option value="DELETE">DELETE</option>
      </select>

      <input
        type="number"
        name="statusCode"
        min="100"
        max="599"
        value={form.statusCode}
        onChange={handleChange}
        placeholder="Status"
      />

      <button type="submit">검색</button>
      <button type="button" onClick={handleReset}>초기화</button>
    </form>
      <h2 style={{ color: '#007bff' }}>📑 전체 로그 ({pageInfo.totalElements})</h2>
      <table border="1" style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead style={{ backgroundColor: '#eef6ff' }}>
          <tr>
            <th>로그 번호</th>
            <th>IP</th>
            <th>Method</th>
            <th>URL</th>
            <th>Status</th>
            <th>Time</th>
          </tr>
        </thead>
        <tbody>
          {logs.map(log => (
            // 마우스 클릭과 키보드 선택 모두 동일한 상세 모달을 연다.
            <tr
              key={log.id}
              tabIndex="0"
              onClick={() => onLogSelect?.(log.id)}
              onKeyDown={event => {
                if (event.key === 'Enter' || event.key === ' ') onLogSelect?.(log.id);
              }}
              style={{ textAlign: 'center', cursor: 'pointer' }}
            >
              <td>{log.id}</td>
              <td>{log.ipAddress}</td>
              <td>{log.requestMethod}</td>
              <td>{log.requestUrl}</td>
              <td style={{ color: log.statusCode === 403 ? 'red' : 'black' }}>{log.statusCode}</td>
              <td>{new Date(log.createdAt).toLocaleString()}</td>
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

export default LogTable;
