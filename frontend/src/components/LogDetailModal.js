import React, {useEffect, useState} from 'react';
import axios from 'axios';

function formatRawLog(rawLog) {
  if (!rawLog) return '-';

  try {
    return JSON.stringify(JSON.parse(rawLog), null, 2);
  } catch {
    // JSON이 아닌 레거시 로그도 원문 그대로 확인할 수 있게 한다.
    return rawLog;
  }
}

function LogDetailModal({logId, onClose}) {
  const [log, setLog] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (logId == null) return;

    setLoading(true);
    setError('');
    setLog(null);

    // 목록에는 없는 rawLog를 선택 시점에만 조회한다.
    axios.get(`${process.env.REACT_APP_API_BASE_URL}/api/v1/logs/${logId}`)
      .then(response => setLog(response.data))
      .catch(() => setError('로그 상세 정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, [logId]);

  if (logId == null) return null;

  return (
    <div
      role="presentation"
      onClick={onClose}
      style={{
        position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.45)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="log-detail-title"
        onClick={event => event.stopPropagation()}
        style={{
          backgroundColor: 'white', width: 'min(900px, 90vw)', maxHeight: '85vh',
          overflow: 'auto', padding: '24px', borderRadius: '10px',
        }}
      >
        <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
          <h2 id="log-detail-title">로그 상세 #{logId}</h2>
          <button type="button" onClick={onClose} aria-label="로그 상세 닫기">닫기</button>
        </div>

        {loading && <p>불러오는 중...</p>}
        {error && <p role="alert" style={{color: 'red'}}>{error}</p>}
        {log && (
          <>
            <dl style={{display: 'grid', gridTemplateColumns: '120px 1fr', gap: '8px'}}>
              <dt>IP</dt><dd>{log.ipAddress}</dd>
              <dt>Method</dt><dd>{log.requestMethod}</dd>
              <dt>URL</dt><dd style={{wordBreak: 'break-all'}}>{log.requestUrl}</dd>
              <dt>Status</dt><dd>{log.statusCode}</dd>
              <dt>수집 시간</dt><dd>{log.createdAt ? new Date(log.createdAt).toLocaleString() : '-'}</dd>
            </dl>
            <h3>전체 로그</h3>
            <pre style={{backgroundColor: '#f5f5f5', padding: '16px', overflow: 'auto', textAlign: 'left'}}>
              {formatRawLog(log.rawLog)}
            </pre>
          </>
        )}
      </section>
    </div>
  );
}

export default LogDetailModal;
