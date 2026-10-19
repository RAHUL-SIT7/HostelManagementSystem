USE hostel_management;

-- 1. Users and authentication
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'STUDENT') NOT NULL,
    account_status ENUM(
        'PENDING', 'ACTIVE', 'REJECTED', 'INACTIVE'
    ) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

-- 2. Student profile
CREATE TABLE IF NOT EXISTS students (
    student_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    profession ENUM('STUDENT', 'OFFICE', 'OTHER') NOT NULL,
    institution_name VARCHAR(150),
    date_of_birth DATE,
    blood_group VARCHAR(5),
    guardian_name VARCHAR(120),
    guardian_phone VARCHAR(20),
    citizenship_id VARCHAR(100),
    national_id VARCHAR(100),
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 3. Hostel infrastructure
CREATE TABLE IF NOT EXISTS hostels (
    hostel_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hostel_name VARCHAR(120) NOT NULL,
    hostel_code VARCHAR(30) NOT NULL UNIQUE,
    address VARCHAR(255),
    hostel_type VARCHAR(30),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS blocks (
    block_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    hostel_id BIGINT NOT NULL,
    block_name VARCHAR(80) NOT NULL,
    block_code VARCHAR(30) NOT NULL,
    FOREIGN KEY (hostel_id) REFERENCES hostels(hostel_id),
    UNIQUE (hostel_id, block_code)
);

CREATE TABLE IF NOT EXISTS floors (
    floor_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    block_id BIGINT NOT NULL,
    floor_number INT NOT NULL,
    floor_name VARCHAR(50),
    FOREIGN KEY (block_id) REFERENCES blocks(block_id),
    UNIQUE (block_id, floor_number)
);

CREATE TABLE IF NOT EXISTS room_types (
    room_type_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_name VARCHAR(50) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    default_monthly_fee DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (capacity > 0),
    CHECK (default_monthly_fee >= 0)
);

CREATE TABLE IF NOT EXISTS rooms (
    room_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    floor_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    room_number VARCHAR(30) NOT NULL,
    room_status ENUM(
        'ACTIVE', 'MAINTENANCE', 'INACTIVE'
    ) NOT NULL DEFAULT 'ACTIVE',
    FOREIGN KEY (floor_id) REFERENCES floors(floor_id),
    FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id),
    UNIQUE (floor_id, room_number)
);

CREATE TABLE IF NOT EXISTS beds (
    bed_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    bed_number VARCHAR(20) NOT NULL,
    bed_status ENUM(
        'AVAILABLE', 'MAINTENANCE', 'INACTIVE'
    ) NOT NULL DEFAULT 'AVAILABLE',
    FOREIGN KEY (room_id) REFERENCES rooms(room_id),
    UNIQUE (room_id, bed_number)
);

-- 4. Student room allocation
CREATE TABLE IF NOT EXISTS room_allocations (
    allocation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    bed_id BIGINT NOT NULL,
    allocated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP NULL,
    allocation_status ENUM(
        'ACTIVE', 'RELEASED', 'CANCELLED'
    ) NOT NULL DEFAULT 'ACTIVE',
    allocated_by BIGINT NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (bed_id) REFERENCES beds(bed_id),
    FOREIGN KEY (allocated_by) REFERENCES users(user_id)
);

-- 5. Fee plans
CREATE TABLE IF NOT EXISTS fee_plans (
    fee_plan_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_type_id BIGINT NOT NULL,
    monthly_amount DECIMAL(10,2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id),
    CHECK (monthly_amount >= 0)
);

-- 6. Payments
CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    fee_plan_id BIGINT,
    billing_month DATE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    transaction_id VARCHAR(120) NOT NULL UNIQUE,
    payment_date DATE NOT NULL,
    proof_path VARCHAR(500),
    payment_status ENUM(
        'PENDING', 'APPROVED', 'REJECTED'
    ) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    admin_remarks VARCHAR(500),
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (fee_plan_id) REFERENCES fee_plans(fee_plan_id),
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
    CHECK (amount > 0)
);

-- 7. Leave requests
CREATE TABLE IF NOT EXISTS leave_requests (
    leave_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    leave_status ENUM(
        'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
    CHECK (end_date >= start_date)
);

-- 8. Complaints
CREATE TABLE IF NOT EXISTS complaints (
    complaint_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    room_id BIGINT NULL,
    category ENUM(
        'PLUMBING', 'ELECTRICAL', 'CARPENTER',
        'CLEANING', 'OTHER'
    ) NOT NULL,
    other_professional VARCHAR(100),
    description TEXT NOT NULL,
    complaint_status ENUM(
        'PENDING', 'IN_PROGRESS', 'SOLVED', 'REJECTED'
    ) NOT NULL DEFAULT 'PENDING',
    resolution_note VARCHAR(1000),
    resolved_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (room_id) REFERENCES rooms(room_id),
    FOREIGN KEY (resolved_by) REFERENCES users(user_id)
);

-- 9. Food menu: one entry per day and meal
CREATE TABLE IF NOT EXISTS food_menu (
    menu_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    day_of_week TINYINT NOT NULL,
    meal_type ENUM(
        'BREAKFAST', 'LUNCH', 'SNACK', 'DINNER'
    ) NOT NULL,
    menu_description TEXT NOT NULL,
    meal_time TIME,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (updated_by) REFERENCES users(user_id),
    UNIQUE (day_of_week, meal_type),
    CHECK (day_of_week BETWEEN 1 AND 7)
);

-- 10. In-app notifications
CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);



USE hostel_management;
SHOW TABLES;