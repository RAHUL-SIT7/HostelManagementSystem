package com.hostelmanagement.service;

import com.hostelmanagement.auth.PasswordUtil;
import com.hostelmanagement.dao.StudentRegistrationDAO;
import com.hostelmanagement.model.Room;
import com.hostelmanagement.model.RoomType;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class StudentRegistrationService {

    private final StudentRegistrationDAO dao = new StudentRegistrationDAO();

    public List<RoomType> getActiveRoomTypes() throws SQLException {
        return dao.getActiveRoomTypes();
    }

    public List<Room> getAvailableRoomsByType(long roomTypeId)
            throws SQLException {

        if (roomTypeId <= 0) {
            throw new IllegalArgumentException("Select a room type.");
        }

        return dao.getAvailableRoomsByType(roomTypeId);
    }

    public void registerStudent(
            String fullName,
            String address,
            String phone,
            String email,
            String password,
            String profession,
            String institutionName,
            String organizationName,
            LocalDate dateOfBirth,
            String bloodGroup,
            String guardianName,
            String guardianPhone,
            String citizenshipId,
            String nationalId,
            long roomTypeId,
            long roomId
    ) throws SQLException {

        requireText(fullName, "Full name", 120);
        requireText(address, "Address", 255);
        requireText(phone, "Phone", 20);
        requireText(email, "Email", 254);
        requireText(guardianName, "Guardian name", 120);
        requireText(guardianPhone, "Guardian phone", 20);
        requireText(citizenshipId, "Citizenship ID", 100);
        requireText(nationalId, "National ID", 100);
        requireText(bloodGroup, "Blood group", 5);

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (!normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }

        if (profession == null
                || (!profession.equals("STUDENT")
                && !profession.equals("OFFICE"))) {
            throw new IllegalArgumentException(
                    "Select Student or Office."
            );
        }

        String normalizedInstitution = null;
        String normalizedOrganization = null;

        if ("STUDENT".equals(profession)) {
            requireText(institutionName, "Institute name", 150);
            normalizedInstitution = institutionName.trim();
        } else {
            requireText(organizationName, "Organization name", 150);
            normalizedOrganization = organizationName.trim();
        }

        if (dateOfBirth == null || !dateOfBirth.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Enter a valid date of birth in the past."
            );
        }

        if (phone.trim().length() < 7
                || guardianPhone.trim().length() < 7) {
            throw new IllegalArgumentException(
                    "Phone numbers must contain at least 7 characters."
            );
        }

        if (roomTypeId <= 0 || roomId <= 0) {
            throw new IllegalArgumentException(
                    "Select a room type and a preferred available room."
            );
        }

        String passwordHash = PasswordUtil.hashPassword(password);

        dao.submitApplication(
                normalizedEmail,
                passwordHash,
                fullName.trim(),
                address.trim(),
                phone.trim(),
                profession,
                normalizedInstitution,
                normalizedOrganization,
                java.sql.Date.valueOf(dateOfBirth),
                bloodGroup,
                guardianName.trim(),
                guardianPhone.trim(),
                citizenshipId.trim(),
                nationalId.trim(),
                roomTypeId,
                roomId
        );
    }

    private void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }

        if (value.trim().length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " must not exceed " + maxLength + " characters."
            );
        }
    }
}