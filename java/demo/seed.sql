-- Optional demo data. Run after application startup has applied Flyway migrations.
-- Rerunning preserves existing rows and any interest already accrued.
BEGIN;
INSERT INTO "timeDeposits" (id, "planType", days, balance) VALUES
    (1, 'basic', 31, 1200.00),
    (2, 'student', 365, 1200.00),
    (3, 'premium', 46, 1200.00),
    (4, 'student', 366, 1200.00),
    (5, 'premium', 45, 1200.00),
    (6, 'unknown', 60, 1200.00)
ON CONFLICT (id) DO NOTHING;

INSERT INTO withdrawals (id, "timeDepositId", amount, date) VALUES
    (10, 1, 25.00, '2026-01-15'),
    (11, 1, 5.50, '2026-01-20'),
    (20, 2, 10.00, '2026-02-01')
ON CONFLICT (id) DO NOTHING;
COMMIT;
