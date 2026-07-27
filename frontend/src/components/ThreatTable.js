import React, {useState} from 'react';

function ThreatTable ( {
  threats,
  isNewThreat,
  pageInfo,
  onPageChange,
  onSearch,
  onReset,
})
{
  const [form,setForm] = useState({
    threatType: "",
    severity: "",
  });

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
    const emptyForm = {threatType: "", severity: ""};
    setForm(emptyForm);
    onReset();
  };

return (
        <section style={{ 
          backgroundColor: 'white', padding: '15px', borderRadius: '10px',
           boxShadow: '0 2px 5px rgba(0,0,0,0.1)',
           border: isNewThreat ? '2px solid red' : '1px solid transparent', 
           transition: 'all 0.3s ease' }}>
        <form onSubmit={handleSubmit}>
          <input
            name="threatType"
            value={form.threatType}
            onChange={handleChange}
            placeholder="Threat type"
          />

          <select
            name="severity"
            value={form.severity}
            onChange={handleChange}
          >
            <option value="">All severity</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HIGH">HIGH</option>
            <option value="CRITICAL">CRITICAL</option>
          </select>

          <button type="submit">검색</button>
          <button type="button" onClick={handleReset}>초기화</button>
        </form>

          <h2 style={{ color: '#d9534f' }}>
            🚨 위협 탐지 ({pageInfo.totalElements})
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
)
}

export default ThreatTable;