import React,{useState} from 'react';

function BlacklistTable({ blacklists,pageInfo,onPageChange,onSearch,onReset,onViewLog }) {
  const [form,setForm] = useState({
    ip: "",
    dangerLevel: "",
  });

  const getDangerLevelLabel = dangerLevel => {
    if(dangerLevel >=4){
      return 'CRITICAL';
    }

    if(dangerLevel >=3){
      return 'HIGH';
    }

    return 'MEDIUM';
  };

  const handleChange = event => {
    const {name, value} = event.target;

    setForm(previous => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleSubmit = event => {
    event.preventDefault();
    onSearch(form);
  };

  const handleReset = () =>{
    const emptyForm = {ip: "", dangerLevel: ""};
    setForm(emptyForm);
    onReset();
  };
  return (
    <section style={{ backgroundColor: 'white', padding: '15px', borderRadius: '10px', boxShadow: '0 2px 5px rgba(0,0,0,0.1)' }}>
      <form onSubmit={handleSubmit}>
        <input
          name="ip"
          value={form.ip}
          onChange={handleChange}
          placeholder="IP address"
        />

        <select
          name="dangerLevel"
          value={form.dangerLevel}
          onChange={handleChange}
        >
          <option value="">All danger levels</option>
          {[1, 2, 3, 4, 5].map(level => (
            <option key={level} value={level}>{level}</option>
          ))}
        </select>

        <button type="submit">검색</button>
        <button type="button" onClick={handleReset}>초기화</button>
    </form>
      <h2 style={{ color: '#333' }}>🚫 차단 IP ({pageInfo.totalElements})</h2>
      <table border="1" style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead style={{ backgroundColor: '#f8f9fa' }}>
          <tr>
            <th>원인 위협</th>
            <th>원본 로그</th>
            <th>IP 주소</th>
            <th>차단 사유</th>
            <th>위험도</th>
          </tr>
        </thead>
        <tbody>
          {blacklists.map(b => (
            <tr key={b.id} style={{ textAlign: 'center' }}>
              <td>
                {b.sourceThreatId !=null
                ? `#${b.sourceThreatId}`
                : '-'
              }
              </td>
              <td>
                {b.logId !=null ? (
                <button
                  type="button"
                  onClick={() => onViewLog(b.logId)}
                  title = {`원본 로그 #${b.logId} 보기`}
                  aria-label = {`원본 로그 #${b.logId} 보기`}
                >
                    #{b.logId}
                </button>  
                ) : b.sourceThreatId != null ?(
                  `원본 로그 없음`
                ) : (
                  `수동 등록`
                )}
              </td>

              <td style={{ fontWeight: 'bold' }}>{b.ipAddress}</td>
              <td>{b.reason}</td>
              <td
                style = {{
                  color: b.dangerLevel >=4 ? 'red' : 'orange',
                  fontWeight: 'bold',
                }}
              >
                {getDangerLevelLabel(b.dangerLevel)} ({b.dangerLevel})
                </td>
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