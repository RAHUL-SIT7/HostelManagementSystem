USE hostel_management;

-- Additional details for student registration
ALTER TABLE students
    ADD COLUMN organization_name VARCHAR(150) NULL,
    ADD COLUMN preferred_room_type_id BIGINT NULL,
    ADD COLUMN preferred_room_id BIGINT NULL;

-- Connect the preferred room type to the room types table
ALTER TABLE students
    ADD CONSTRAINT fk_student_preferred_room_type
    FOREIGN KEY (preferred_room_type_id)
    REFERENCES room_types(room_type_id);

-- Connect the preferred room to the rooms table
ALTER TABLE students
    ADD CONSTRAINT fk_student_preferred_room
    FOREIGN KEY (preferred_room_id)
    REFERENCES rooms(room_id);