INSERT INTO api_provider_keys (id, provider_name, encrypted_api_key, active, created_at, updated_at)
VALUES
    (nextval('api_provider_keys_seq'), 'Fixer.io', 'encrypted-fixer-test-key', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (nextval('api_provider_keys_seq'), 'ExchangeRatesAPI', 'encrypted-exchangerates-test-key', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (nextval('api_provider_keys_seq'), 'CurrencyAPI', 'encrypted-currencyapi-test-key', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO supported_currencies (id, currency_code)
VALUES
    (nextval('supported_currencies_seq'), 'USD'),
    (nextval('supported_currencies_seq'), 'EUR'),
    (nextval('supported_currencies_seq'), 'GBP'),
    (nextval('supported_currencies_seq'), 'JPY'),
    (nextval('supported_currencies_seq'), 'CHF'),
    (nextval('supported_currencies_seq'), 'CAD'),
    (nextval('supported_currencies_seq'), 'AUD'),
    (nextval('supported_currencies_seq'), 'CNY'),
    (nextval('supported_currencies_seq'), 'SEK'),
    (nextval('supported_currencies_seq'), 'NZD');
