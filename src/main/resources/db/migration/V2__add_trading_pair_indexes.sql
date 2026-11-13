CREATE INDEX idx_trading_pair_fetched_at ON trading_pair (fetched_at);
CREATE UNIQUE INDEX uq_trading_pair_symbol_fetched_at ON trading_pair (symbol, fetched_at);
