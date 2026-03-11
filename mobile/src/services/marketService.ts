import api from '../api/client';
import { StockQuote, SearchResult } from '../types';

export const marketService = {
  /**
   * Fetch live prices for given tickers.
   * Backend handles: Redis cache check → Twelve Data API → cache store.
   */
  async getQuotes(tickers: string[]): Promise<Record<string, StockQuote>> {
    if (tickers.length === 0) return {};
    const { data } = await api.get('/market/quotes', {
      params: { tickers: tickers.join(',') },
    });
    return data;
  },

  /**
   * Search EGX stocks by ticker or company name.
   */
  async searchSymbols(query: string): Promise<SearchResult[]> {
    if (!query || query.length < 1) return [];
    const { data } = await api.get('/market/search', {
      params: { q: query },
    });
    return data;
  },

  /**
   * Validate a Twelve Data API key.
   * Returns { valid: boolean, message: string, testQuote?: StockQuote }
   */
  async validateApiKey(apiKey: string): Promise<{
    valid: boolean;
    message: string;
    testQuote?: StockQuote;
  }> {
    const { data } = await api.post('/market/validate-key', { apiKey });
    return data;
  },
};
