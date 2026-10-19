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
      'food_menu',
      'notifications',
      'complaints',
      'leave_requests'
  )
ORDER BY TABLE_NAME, ORDINAL_POSITION;
USE hostel_management;

SHOW CREATE TABLE food_menu;
USE hostel_management;

-- One feedback per student, per menu, per date.
CREATE TABLE IF NOT EXISTS food_feedback (
    feedback_id BIGINT NOT NULL AUTO_INCREMENT,
    menu_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    feedback_date DATE NOT NULL,
    rating ENUM('GOOD', 'AVERAGE', 'SUGGESTIONS') NOT NULL,
    comments TEXT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (feedback_id),

    UNIQUE KEY uq_food_feedback_daily (
        menu_id,
        student_id,
        feedback_date
    ),

    KEY idx_food_feedback_date (feedback_date),

    CONSTRAINT fk_food_feedback_menu
        FOREIGN KEY (menu_id)
        REFERENCES food_menu(menu_id),

    CONSTRAINT fk_food_feedback_student
        FOREIGN KEY (student_id)
        REFERENCES students(student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Stores whether a student expects to attend a meal.
-- The same student can update their attendance for that meal.
CREATE TABLE IF NOT EXISTS meal_attendance (
    attendance_id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    meal_date DATE NOT NULL,
    meal_type ENUM(
        'BREAKFAST',
        'LUNCH',
        'SNACK',
        'DINNER'
    ) NOT NULL DEFAULT 'DINNER',
    will_attend TINYINT(1) NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (attendance_id),

    UNIQUE KEY uq_student_meal_attendance (
        student_id,
        meal_date,
        meal_type
    ),

    KEY idx_meal_attendance_date (meal_date),

    CONSTRAINT fk_meal_attendance_student
        FOREIGN KEY (student_id)
        REFERENCES students(student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
USE hostel_management;

ALTER TABLE food_menu
    ADD COLUMN meal_start_time TIME NULL AFTER meal_time,
    ADD COLUMN meal_end_time TIME NULL AFTER meal_start_time;
show tables;
desc table food_menu;

USE hostel_management;

SHOW COLUMNS FROM food_menu LIKE 'meal_start_time';
SHOW COLUMNS FROM food_menu LIKE 'meal_end_time';

USE hostel_management;

CREATE TABLE IF NOT EXISTS meal_time_slots (
    schedule_id BIGINT NOT NULL AUTO_INCREMENT,
    meal_type ENUM(
        'BREAKFAST', 'LUNCH', 'SNACK', 'DINNER'
    ) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (schedule_id),
    UNIQUE KEY uq_meal_time_slots_meal_type (meal_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO meal_time_slots
    (meal_type, start_time, end_time)
VALUES
    ('BREAKFAST', '07:00:00', '09:00:00'),
    ('LUNCH',     '12:00:00', '14:00:00'),
    ('SNACK',     '16:00:00', '17:00:00'),
    ('DINNER',    '19:00:00', '21:00:00');