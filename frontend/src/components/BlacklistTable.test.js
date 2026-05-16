import {render,screen} from '@testing-library/react';
import BlacklistTable from './BlacklistTable';

test('차단된 IP 정보를 화면에 표시한다', () => {
    const blacklists = [
     {
        id: 1,
        ipAddress: '127.0.0.1',
        reason: 'AI 탐지',
        dangerLevel: 4
    }
];


    render(<BlacklistTable blacklists={blacklists}/>);

    expect(screen.getByText(/현재 차단된 IP/)).toBeInTheDocument();
    expect(screen.getByText('127.0.0.1')).toBeInTheDocument();
    expect(screen.getByText('AI 탐지')).toBeInTheDocument();
    expect(screen.getByText('4')).toBeInTheDocument();

});


/* 
    1. 가짜 데이터 준비
    2. 컴포넌트에 props로 전달
    3. render()로 화면에 그림
    4. screen.getByText()로 텍스트가 있는지 확인

실행방법:
    cmd 창에서 npm test 입력
*/