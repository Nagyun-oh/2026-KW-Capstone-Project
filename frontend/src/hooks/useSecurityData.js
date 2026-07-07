import { useState } from 'react';
import axios from 'axios';

function useSecurityData() {

  /* == 정보 및 페이지 상태 ( 전체 로그, 위협, 블랙리스트) == */
  const [logs, setLogs] = useState([]);
  const [logPage,setLogPage] = useState({
    number:0,
    totalPages:0,        // 전체 페이지 수
    totalElements:0,    // 전체 개수
    size:20,            // 한 페이지의 최대 데이터 개수
  });

  const [threats, setThreats] = useState([]);
  const [threatPage,setThreatPage] = useState({
    number:0,
    totalPages:0,
    totalElements:0,
    size:20,
  });

  const [blacklists, setBlacklists] = useState([]);
  const [blacklistPage,setBlackListPage] = useState({
    number:0,
    totalPages:0,
    totalElements:0,
    size:20,
  });
 
  /* == 조회 함수 ( 전체 로그, 위협, 블랙리스트) == */
  const fetchLogs = (page =0) => {
    axios.get(
      `${process.env.REACT_APP_API_BASE_URL}/api/v1/logs`,
      {
        params: {
          page: page,
          size: 20,
        },
      }
    )
    .then(response => {
      // 현재 페이지의 로그 목록 저장
      setLogs(response.data.content ?? []);

      // 백엔드 Page 응답의 페이지 정보 저장
      setLogPage({
        number: response.data.number,
        totalPages: response.data.totalPages,
        totalElements: response.data.totalElements,
        size: response.data.size,
      });
    })
    .catch(error =>{
      console.error("로그 조회 실패",error);
    });
  };

  const fetchThreats = (page =0) => {
    axios.get(
      `${process.env.REACT_APP_API_BASE_URL}/api/v1/threats`,
      {
        params: {
          page: page,
          size: 20,
        },
      }
    )
    .then(response => {
      // 현재 페이지의 로그 목록 저장
      setThreats(response.data.content ?? []);

      // 백엔드 Page 응답의 페이지 정보 저장
      setThreatPage({
        number: response.data.number,
        totalPages: response.data.totalPages,
        totalElements: response.data.totalElements,
        size: response.data.size,
      });
    })
    .catch(error =>{
      console.error("위협 조회 실패",error);
    });
  };

  const fetchBlacklists = (page =0) => {
    axios.get(
      `${process.env.REACT_APP_API_BASE_URL}/api/v1/blacklist`,
      {
        params: {
          page: page,
          size: 20,
        },
      }
    )
    .then(response => {
      // 현재 페이지의 로그 목록 저장
      setBlacklists(response.data.content ?? []);

      // 백엔드 Page 응답의 페이지 정보 저장
      setBlackListPage({
        number: response.data.number,
        totalPages: response.data.totalPages,
        totalElements: response.data.totalElements,
        size: response.data.size,
      });
    })
    .catch(error =>{
      console.error("블랙리스트 조회 실패",error);
    });
  };
  
  /* 전체 새로고침 */
  const fetchAllData = () => {
    fetchLogs(0);
    fetchThreats(0);
    fetchBlacklists(0);
  };

  /* 
    반환 객체
      사용하는 이유 : 커스텀 Hook 내부 값은 기본적으로 외부에서 접근 할 수 없음.
                      따라서, return에 포함해야 App.js에서 사용할 수 있음.
  */
  return { 
      logs, 
      threats,
      blacklists, 
      logPage,
      threatPage,
      blacklistPage,
      fetchLogs,
      fetchThreats,
      fetchBlacklists,
      fetchAllData 
  };
}

export default useSecurityData;

/* 
  useSecurityData.js : 서버 데이터와 페이지 상태 관리  
*/