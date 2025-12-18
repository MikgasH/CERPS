INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'USD', 'EUR', 1.10, 'TEST', DATEADD('DAY', -7, CURRENT_TIMESTAMP()));

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'USD', 'EUR', 1.12, 'TEST', DATEADD('DAY', -5, CURRENT_TIMESTAMP()));

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'USD', 'EUR', 1.15, 'TEST', DATEADD('DAY', -3, CURRENT_TIMESTAMP()));

INSERT INTO exchange_rates (id, base_currency, target_currency, rate, source, timestamp)
VALUES (RANDOM_UUID(), 'USD', 'EUR', 1.18, 'TEST', CURRENT_TIMESTAMP());

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
