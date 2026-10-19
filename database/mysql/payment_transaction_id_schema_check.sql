-- Hostel Management System: payment transaction ID protection
-- Run these diagnostics in MySQL Workbench against your hostel_management schema.
-- Do NOT run the ALTER TABLE statement at the end until you have checked the results.

-- 1) Look for duplicate references after trimming and ignoring letter case.
-- This query should return ZERO rows before adding the unique index.
use hostel_management;
SELECT
    LOWER(TRIM(transaction_id)) AS normalized_transaction_id,
    COUNT(*) AS occurrences,
    GROUP_CONCAT(payment_id ORDER BY payment_id) AS payment_ids
FROM payments
WHERE transaction_id IS NOT NULL
  AND TRIM(transaction_id) <> ''
GROUP BY LOWER(TRIM(transaction_id))
HAVING COUNT(*) > 1;

-- 2) Inspect the indexes already present on payments.
-- If an existing UNIQUE index already covers transaction_id, do not add another one.
SHOW INDEX FROM payments;

-- 3) ONLY after query 1 returns no rows AND query 2 confirms there is no unique
-- index on transaction_id, run this statement manually:
-- ALTER TABLE payments ADD UNIQUE KEY uq_payments_transaction_id (transaction_id);
