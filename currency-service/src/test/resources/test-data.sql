INSERT INTO api_provider_keys (id, provider_name, encrypted_api_key, active, created_at, updated_at)
VALUES (1, 'Fixer.io', 'encrypted-fixer-test-key', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO api_provider_keys (id, provider_name, encrypted_api_key, active, created_at, updated_at)
VALUES (2, 'ExchangeRatesAPI', 'encrypted-exchangerates-test-key', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO api_provider_keys (id, provider_name, encrypted_api_key, active, created_at, updated_at)
VALUES (3, 'CurrencyAPI', 'encrypted-currencyapi-test-key', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO supported_currencies (id, currency_code) VALUES (1, 'USD');
INSERT INTO supported_currencies (id, currency_code) VALUES (2, 'EUR');
INSERT INTO supported_currencies (id, currency_code) VALUES (3, 'GBP');
INSERT INTO supported_currencies (id, currency_code) VALUES (4, 'JPY');
INSERT INTO supported_currencies (id, currency_code) VALUES (5, 'CHF');
INSERT INTO supported_currencies (id, currency_code) VALUES (6, 'CAD');
INSERT INTO supported_currencies (id, currency_code) VALUES (7, 'AUD');
INSERT INTO supported_currencies (id, currency_code) VALUES (8, 'CNY');
INSERT INTO supported_currencies (id, currency_code) VALUES (9, 'SEK');
INSERT INTO supported_currencies (id, currency_code) VALUES (10, 'NZD');
