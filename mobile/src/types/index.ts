// ── Auth ──
export interface AuthResponse {
  userId: string;
  email: string;
  displayName: string;
  authProvider: 'GOOGLE' | 'APPLE';
  profileImageUrl: string | null;
  biometricEnabled: boolean;
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresIn: number;
  isNewUser: boolean;
}

export interface User {
  id: string;
  email: string;
  displayName: string;
  authProvider: 'GOOGLE' | 'APPLE';
  profileImageUrl: string | null;
  biometricEnabled: boolean;
}

// ── Portfolio ──
export interface PortfolioResponse {
  portfolioId: string;
  name: string;
  totalInvested: number;
  currentValue?: number;
  totalProfitLoss?: number;
  totalProfitLossPercent?: number;
  positionCount: number;
  positions: Position[];
}

export interface Position {
  ticker: string;
  stockName: string;
  totalShares: number;
  weightedAvgCost: number;
  totalInvested: number;
  currentPrice?: number;
  currentValue?: number;
  profitLoss?: number;
  profitLossPercent?: number;
  allocationPercent?: number;
  orderCount: number;
  orders: OrderItem[];
}

export interface OrderItem {
  id: string;
  ticker: string;
  stockName: string;
  quantity: number;
  pricePerShare: number;
  commission: number;
  totalCost: number;
  buyDate: string;
  notes: string;
}

export interface CreateOrderRequest {
  ticker: string;
  stockName?: string;
  quantity: number;
  pricePerShare: number;
  commission?: number;
  buyDate: string;
  notes?: string;
}

// ── API ──
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}

// ── Market Data (from Twelve Data via backend) ──
export interface StockQuote {
  ticker: string;
  name: string;
  exchange: string;
  currency: string;
  open: number;
  high: number;
  low: number;
  close: number;        // current / last price
  previousClose: number;
  volume: number;
  change: number;
  percentChange: number;
  marketOpen: boolean;
}

export interface SearchResult {
  ticker: string;
  name: string;
  exchange: string;
  type: string;
}
