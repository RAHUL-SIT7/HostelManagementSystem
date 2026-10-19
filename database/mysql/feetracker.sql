USE hostel_management;

SELECT
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'hostel_management'
  AND TABLE_NAME IN (
      'fee_plans',
      'payments',
      'students',
      'users'
  )
ORDER BY TABLE_NAME, ORDINAL_POSITION;