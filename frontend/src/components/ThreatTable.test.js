import {fireEvent,render,screen} from '@testing-library/react';
import ThreatTable from './ThreatTable';

test('탐지된 위협 정보를 화면에 표시한다', () => {
    const threats = [
        {
            id: 1,
            logId: 152,
            threatScore: 0.9123,
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
    const onLogSelect = jest.fn();

    render(
    <ThreatTable
        threats = {threats}
        isNewThreat={true}
        pageInfo={pageInfo}
        onPageChange={onPageChange}
        onLogSelect={onLogSelect}
    />
    );

    expect(screen.getByText(/위협 탐지/)).toBeInTheDocument();
    expect(screen.getByText('152')).toBeInTheDocument();
    expect(screen.getByText('0.9123')).toBeInTheDocument();
    expect(screen.getAllByText('CRITICAL')).toHaveLength(2);
    expect(screen.getByText('URL 공격 키워드')).toBeInTheDocument();
    expect(screen.getByText(/NEW/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', {name: '152'}));
    expect(onLogSelect).toHaveBeenCalledWith(152);
});

/* 
    1. 가짜 데이터 준비
    2. 컴포넌트에 props로 전달
    3. render()로 화면에 그림
    4. screen.getByText()로 텍스트가 있는지 확인

실행방법:
    cmd 창에서 npm test 입력
*/
