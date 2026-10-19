USE hostel_management;

SELECT
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'hostel_management'
  AND TABLE_NAME IN (
      'users',
      'students',
      'rooms',
      'beds',
      'room_allocations'
  )
ORDER BY TABLE_NAME, ORDINAL_POSITION;