INSERT INTO api_provider_keys (id, provider_name, encrypted_api_key, active, created_at, updated_at)
VALUES
    (1, 'Fixer.io', 'encrypted-fixer-test-key', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'ExchangeRatesAPI', 'encrypted-exchangerates-test-key', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'CurrencyAPI', 'encrypted-currencyapi-test-key', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO supported_currencies (id, currency_code)
VALUES
    (1, 'USD'),
    (2, 'EUR'),
    (3, 'GBP'),
    (4, 'JPY'),
    (5, 'CHF'),
    (6, 'CAD'),
    (7, 'AUD'),
    (8, 'CNY'),
    (9, 'SEK'),
    (10, 'NZD');
