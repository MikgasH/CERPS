CREATE SEQUENCE IF NOT EXISTS supported_currencies_seq START WITH 1 INCREMENT BY 1;

MERGE INTO supported_currencies (id, currency_code) KEY(currency_code)
VALUES (NEXT VALUE FOR supported_currencies_seq, 'USD');

MERGE INTO supported_currencies (id, currency_code) KEY(currency_code)
VALUES (NEXT VALUE FOR supported_currencies_seq, 'EUR');

MERGE INTO supported_currencies (id, currency_code) KEY(currency_code)
VALUES (NEXT VALUE FOR supported_currencies_seq, 'GBP');

MERGE INTO supported_currencies (id, currency_code) KEY(currency_code)
VALUES (NEXT VALUE FOR supported_currencies_seq, 'JPY');

MERGE INTO supported_currencies (id, currency_code) KEY(currency_code)
VALUES (NEXT VALUE FOR supported_currencies_seq, 'CHF');

MERGE INTO supported_currencies (id, currency_code) KEY(currency_code)
VALUES (NEXT VALUE FOR supported_currencies_seq, 'CAD');

DELETE FROM exchange_rates;

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'EUR', 'USD', 0.909091, 'TEST', DATEADD('DAY', -7, CURRENT_TIMESTAMP()));

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'EUR', 'USD', 0.892857, 'TEST', DATEADD('DAY', -5, CURRENT_TIMESTAMP()));

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'EUR', 'USD', 0.869565, 'TEST', DATEADD('DAY', -3, CURRENT_TIMESTAMP()));

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'EUR', 'USD', 0.847458, 'TEST', CURRENT_TIMESTAMP());

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'EUR', 'GBP', 0.85, 'TEST', DATEADD('DAY', -7, CURRENT_TIMESTAMP()));

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'EUR', 'GBP', 0.87, 'TEST', DATEADD('DAY', -3, CURRENT_TIMESTAMP()));

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'EUR', 'GBP', 0.88, 'TEST', CURRENT_TIMESTAMP());

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'GBP', 'JPY', 145.0, 'TEST', DATEADD('DAY', -7, CURRENT_TIMESTAMP()));

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'GBP', 'JPY', 147.5, 'TEST', DATEADD('DAY', -3, CURRENT_TIMESTAMP()));

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'GBP', 'JPY', 150.0, 'TEST', CURRENT_TIMESTAMP());
