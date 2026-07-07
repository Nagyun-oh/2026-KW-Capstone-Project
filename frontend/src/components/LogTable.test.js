import {render,screen} from '@testing-library/react';
import LogTable from './LogTable';  

// 대시보드 테이블 컴포넌트의 렌더링 검증 테스트 (API 호출 테스트 X)
// -> LogTable이 logs 데이터를 받아서 IP,Method,URL,Status 를 화면에 제대로 출력하는지 검증
test('전체 네트워크 로그를 화면에 표시한다.', () =>{

    // 테스트 로그 데이터 생성
    const logs = [
        {   
            id: 1,
            ipAddress: '127.0.0.1',
            requestMethod: 'GET',
            requestUrl: '/admin',
            statusCode:403,
            createdAt: '2026-05-07T16:00:00'
        }
    ];

    const pageInfo = {
        number:0,
        totalPages:1,
        totalElements:1,
        size:20,
    };
    const onPageChange = jest.fn();

    // LogTable에 logs를 props로 넣어서 화면에 렌더링
    // -> 실제 브라우저를 띄우는게 아니라, 테스트 환경에서 가상의 DOM
    render(
    <LogTable 
        logs={logs}
        pageInfo={pageInfo}
        onPageChange={onPageChange}
    />)
    ; 

    // 화면에 해당 텍스트가 실제로 표시됐는지 확인
    expect(screen.getByText(/전체 네트워크 로그/)).toBeInTheDocument();
    expect(screen.getByText('127.0.0.1')).toBeInTheDocument();
    expect(screen.getByText('GET')).toBeInTheDocument();
    expect(screen.getByText('/admin')).toBeInTheDocument();
    expect(screen.getByText('403')).toBeInTheDocument();
});

/* 
    1. 가짜 데이터 준비
    2. 컴포넌트에 props로 전달
    3. render()로 화면에 그림
    4. screen.getByText()로 텍스트가 있는지 확인

실행방법:
    cmd 창에서 npm test 입력
*/