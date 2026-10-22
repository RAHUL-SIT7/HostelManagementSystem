-- Hostel Management System
-- Supplementary database objects matching Report Sections 19.1-19.4.
-- Run this AFTER the existing hostel_management schema has been created.
-- This script creates/recreates only the objects defined below; it does not
-- drop or modify any existing application tables or data.

USE `hostel_management`;

-- Make this script re-runnable.
DROP PROCEDURE IF EXISTS `sp_hostel_occupancy_summary`;
DROP FUNCTION IF EXISTS `fn_student_monthly_outstanding`;
DROP TRIGGER IF EXISTS `trg_payments_validate_insert`;
DROP VIEW IF EXISTS `vw_hostel_occupancy`;

DELIMITER $$

-- 1) View: one occupancy-summary row per hostel.
-- "beds_without_active_allocation" means beds with no ACTIVE allocation;
-- it does not, by itself, mean the bed's bed_status is AVAILABLE.
CREATE VIEW `vw_hostel_occupancy` AS
SELECT
    h.hostel_id,
    h.hostel_name,
    COUNT(DISTINCT r.room_id) AS total_rooms,
    COUNT(DISTINCT b.bed_id) AS total_beds,
    COUNT(DISTINCT CASE
        WHEN active_alloc.bed_id IS NOT NULL THEN b.bed_id
    END) AS occupied_beds,
    COUNT(DISTINCT CASE
        WHEN b.bed_id IS NOT NULL AND active_alloc.bed_id IS NULL
        THEN b.bed_id
    END) AS beds_without_active_allocation
FROM hostels AS h
LEFT JOIN blocks AS bl
    ON bl.hostel_id = h.hostel_id
LEFT JOIN floors AS f
    ON f.block_id = bl.block_id
LEFT JOIN rooms AS r
    ON r.floor_id = f.floor_id
LEFT JOIN beds AS b
    ON b.room_id = r.room_id
LEFT JOIN (
    SELECT DISTINCT bed_id
    FROM room_allocations
    WHERE allocation_status = 'ACTIVE'
) AS active_alloc
    ON active_alloc.bed_id = b.bed_id
GROUP BY h.hostel_id, h.hostel_name;

-- 2) Stored procedure: group rooms and beds by hostel and return the summary.
-- The aggregation is written in the procedure itself to match Report Section 19.1.
CREATE PROCEDURE `sp_hostel_occupancy_summary`()
BEGIN
    SELECT
        h.hostel_id,
        h.hostel_name,
        COUNT(DISTINCT r.room_id) AS total_rooms,
        COUNT(DISTINCT b.bed_id) AS total_beds,
        COUNT(DISTINCT CASE
            WHEN active_alloc.bed_id IS NOT NULL THEN b.bed_id
        END) AS occupied_beds,
        COUNT(DISTINCT CASE
            WHEN b.bed_id IS NOT NULL AND active_alloc.bed_id IS NULL
            THEN b.bed_id
        END) AS beds_without_active_allocation
    FROM hostels AS h
    LEFT JOIN blocks AS bl
        ON bl.hostel_id = h.hostel_id
    LEFT JOIN floors AS f
        ON f.block_id = bl.block_id
    LEFT JOIN rooms AS r
        ON r.floor_id = f.floor_id
    LEFT JOIN beds AS b
        ON b.room_id = r.room_id
    LEFT JOIN (
        SELECT DISTINCT bed_id
        FROM room_allocations
        WHERE allocation_status = 'ACTIVE'
    ) AS active_alloc
        ON active_alloc.bed_id = b.bed_id
    GROUP BY h.hostel_id, h.hostel_name
    ORDER BY h.hostel_name, h.hostel_id;
END$$

-- 3) Stored function: monthly outstanding amount.
-- Returns NULL when the student has no active allocation or no applicable
-- effective fee plan can be found for the selected billing month.
-- Only APPROVED payments for that student and billing month reduce the balance.
CREATE FUNCTION `fn_student_monthly_outstanding`(
    p_student_id BIGINT,
    p_billing_month DATE
)
RETURNS DECIMAL(10,2)
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_monthly_fee DECIMAL(10,2) DEFAULT NULL;
    DECLARE v_approved_paid DECIMAL(10,2) DEFAULT 0.00;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_monthly_fee = NULL;

    IF p_student_id IS NULL OR p_billing_month IS NULL THEN
        RETURN NULL;
    END IF;

    -- A monthly billing period is represented by its first calendar day.
    IF DAYOFMONTH(p_billing_month) <> 1 THEN
        RETURN NULL;
    END IF;

    SELECT fp.monthly_amount
      INTO v_monthly_fee
    FROM room_allocations AS ra
    JOIN beds AS b
      ON b.bed_id = ra.bed_id
    JOIN rooms AS r
      ON r.room_id = b.room_id
    JOIN fee_plans AS fp
      ON fp.room_type_id = r.room_type_id
     AND fp.effective_from <= p_billing_month
     AND (fp.effective_to IS NULL OR fp.effective_to >= p_billing_month)
    WHERE ra.student_id = p_student_id
      AND ra.allocation_status = 'ACTIVE'
    ORDER BY ra.allocated_at DESC, fp.effective_from DESC, fp.fee_plan_id DESC
    LIMIT 1;

    IF v_monthly_fee IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT COALESCE(SUM(p.amount), 0.00)
      INTO v_approved_paid
    FROM payments AS p
    WHERE p.student_id = p_student_id
      AND p.billing_month = p_billing_month
      AND p.payment_status = 'APPROVED';

    RETURN GREATEST(v_monthly_fee - v_approved_paid, 0.00);
END$$

-- 4) BEFORE INSERT trigger: enforce the report's payment-input rules at DB level.
CREATE TRIGGER `trg_payments_validate_insert`
BEFORE INSERT ON `payments`
FOR EACH ROW
BEGIN
    IF NEW.amount IS NULL OR NEW.amount <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Payment amount must be greater than zero';
    END IF;

    IF NEW.transaction_id IS NULL OR CHAR_LENGTH(TRIM(NEW.transaction_id)) = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Transaction reference must not be empty';
    END IF;

    IF NEW.billing_month IS NULL OR DAYOFMONTH(NEW.billing_month) <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Billing month must be stored as the first day of the month';
    END IF;
END$$

DELIMITER ;

-- Quick verification after the script succeeds:
-- CALL sp_hostel_occupancy_summary();
-- SELECT * FROM vw_hostel_occupancy;
-- SELECT fn_student_monthly_outstanding(<existing_student_id>, '2026-10-01');
-- SHOW TRIGGERS FROM hostel_management;
