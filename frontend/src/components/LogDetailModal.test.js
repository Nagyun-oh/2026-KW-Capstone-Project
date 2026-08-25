import {render, screen, waitFor} from '@testing-library/react';
import axios from 'axios';
import LogDetailModal from './LogDetailModal';

jest.mock('axios');

test('선택한 로그의 전체 데이터를 조회해 표시한다', async () => {
  axios.get.mockResolvedValue({
    data: {
      id: 152,
      ipAddress: '172.18.0.1',
      requestMethod: 'GET',
      requestUrl: '/socket.io/',
      statusCode: 101,
      rawLog: '{"transaction":{"messages":[]}}',
      createdAt: '2026-08-25T20:30:00',
    },
  });

  render(<LogDetailModal logId={152} onClose={jest.fn()} />);

  await waitFor(() => expect(axios.get).toHaveBeenCalledWith(
    `${process.env.REACT_APP_API_BASE_URL}/api/v1/logs/152`
  ));
  expect(await screen.findByText('/socket.io/')).toBeInTheDocument();
  expect(screen.getByText(/"transaction"/)).toBeInTheDocument();
});
