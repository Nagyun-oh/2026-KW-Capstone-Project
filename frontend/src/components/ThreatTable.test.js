import {render,screen} from '@testing-library/react';
import ThreatTable from './ThreatTable';

test('탐지된 위협 정보를 화면에 표시한다', () => {
    const threats = [
        {
            id: 1,
            threatType: 'AI 탐지',
            severity: 'CRITICAL',
            description: 'URL 공격 키워드'
        }
    ];

     const pageInfo = {
        number:0,
        totalPages:1,
        totalElements:1,
        size:20,
    };
    const onPageChange = jest.fn();

    render(
    <ThreatTable
        threats = {threats}
        isNewThreat={true}
        pageInfo={pageInfo}
        onPageChange={onPageChange}
    />
    );

    expect(screen.getByText(/실시간 위협 탐지/)).toBeInTheDocument();
    expect(screen.getByText('AI 탐지')).toBeInTheDocument();
    expect(screen.getByText('CRITICAL')).toBeInTheDocument();
    expect(screen.getByText('URL 공격 키워드')).toBeInTheDocument();
    expect(screen.getByText(/NEW/)).toBeInTheDocument();
});

/* 
    1. 가짜 데이터 준비
    2. 컴포넌트에 props로 전달
    3. render()로 화면에 그림
    4. screen.getByText()로 텍스트가 있는지 확인

실행방법:
    cmd 창에서 npm test 입력
*/