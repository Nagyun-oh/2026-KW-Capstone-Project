import { render, screen } from '@testing-library/react';
import App from './App';

const emptyPage = {
  number: 0,
  totalPages:0,
  totalElements:0,
  size:20,
};

jest.mock("./hooks/useSecurityData.js",()=>({
  __esModule:true,
  default: () => ({
    logs: [],
    threats: [],
    blacklists: [],
    logPage: emptyPage,
    threatPage: emptyPage,
    blacklistPage: emptyPage,
    fetchLogs: jest.fn(),
    fetchThreats: jest.fn(),
    fetchBlacklists: jest.fn(),
    fetchAllData: jest.fn(),
  }),
}));

jest.mock("./hooks/useWebSocket",() => ({
  __esModule: true,
  default: () => ({
    isNewThreat: false,
    connectionStatus: "connected",
  }),
}));

test('대시보드를 화면에 표시한다.', () => {
  render(<App />);
  const linkElement = screen.getByText(/🛡️대시보드/i);
  expect(linkElement).toBeInTheDocument();
});
