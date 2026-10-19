package com.hostelmanagement.model;

public class PendingApplication {

    private final long userId;
    private final long studentId;
    private final String email;
    private final String fullName;
    private final String address;
    private final String phone;
    private final String profession;
    private final String institutionName;
    private final String organizationName;
    private final String dateOfBirth;
    private final String bloodGroup;
    private final String guardianName;
    private final String guardianPhone;
    private final String citizenshipId;
    private final String nationalId;
    private final Long preferredRoomTypeId;
    private final String preferredRoomTypeName;
    private final Long preferredRoomId;
    private final String preferredRoomNumber;
    private final String monthlyFee;
    private final String registeredAt;

    public PendingApplication(
            long userId,
            long studentId,
            String email,
            String fullName,
            String address,
            String phone,
            String profession,
            String institutionName,
            String organizationName,
            String dateOfBirth,
            String bloodGroup,
            String guardianName,
            String guardianPhone,
            String citizenshipId,
            String nationalId,
            Long preferredRoomTypeId,
            String preferredRoomTypeName,
            Long preferredRoomId,
            String preferredRoomNumber,
            String monthlyFee,
            String registeredAt
    ) {
        this.userId = userId;
        this.studentId = studentId;
        this.email = email;
        this.fullName = fullName;
        this.address = address;
        this.phone = phone;
        this.profession = profession;
        this.institutionName = institutionName;
        this.organizationName = organizationName;
        this.dateOfBirth = dateOfBirth;
        this.bloodGroup = bloodGroup;
        this.guardianName = guardianName;
        this.guardianPhone = guardianPhone;
        this.citizenshipId = citizenshipId;
        this.nationalId = nationalId;
        this.preferredRoomTypeId = preferredRoomTypeId;
        this.preferredRoomTypeName = preferredRoomTypeName;
        this.preferredRoomId = preferredRoomId;
        this.preferredRoomNumber = preferredRoomNumber;
        this.monthlyFee = monthlyFee;
        this.registeredAt = registeredAt;
    }

    public long getUserId() { return userId; }
    public long getStudentId() { return studentId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getProfession() { return profession; }
    public String getInstitutionName() { return institutionName; }
    public String getOrganizationName() { return organizationName; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getBloodGroup() { return bloodGroup; }
    public String getGuardianName() { return guardianName; }
    public String getGuardianPhone() { return guardianPhone; }
    public String getCitizenshipId() { return citizenshipId; }
    public String getNationalId() { return nationalId; }

    public Long getPreferredRoomTypeId() {
        return preferredRoomTypeId;
    }

    public String getPreferredRoomTypeName() {
        return preferredRoomTypeName;
    }

    public Long getPreferredRoomId() {
        return preferredRoomId;
    }

    public String getPreferredRoomNumber() {
        return preferredRoomNumber;
    }

    public String getMonthlyFee() { return monthlyFee; }
    public String getRegisteredAt() { return registeredAt; }
}