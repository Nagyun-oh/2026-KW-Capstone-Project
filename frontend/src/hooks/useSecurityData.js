import { useState } from 'react';
import axios from 'axios';

function useSecurityData() {
  const [logs, setLogs] = useState([]);
  const [threats, setThreats] = useState([]);
  const [blacklists, setBlacklists] = useState([]);

  const fetchAllData = () => {
    axios.get(`${process.env.REACT_APP_API_BASE_URL}/api/v1/logs`)
      .then(response => setLogs(response.data))
      .catch(err => console.error("전체 로그 로딩 실패", err));

    axios.get(`${process.env.REACT_APP_API_BASE_URL}/api/v1/threats`)
      .then(response => setThreats(response.data))
      .catch(err => console.error("위협 로그 로딩 실패", err));

    axios.get(`${process.env.REACT_APP_API_BASE_URL}/api/v1/blacklist`)
      .then(response => setBlacklists(response.data))
      .catch(err => console.error("블랙리스트 로딩 실패", err));
  };

  return { logs, threats, blacklists, setLogs,setThreats, setBlacklists,fetchAllData };
}

export default useSecurityData;