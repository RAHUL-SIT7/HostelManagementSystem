# Hostel Management System

A Java desktop application for managing hostel accommodation and day-to-day hostel operations. The application uses Java Swing for its user interface, JDBC for database connectivity, and MySQL for persistent storage.

> **Project note:** Keep this README aligned with the version of the source code and database schema used for the final demonstration. Replace the team placeholders before submitting the repository.

## Features

### Admin
- Manage hostels, blocks, floors, room types, rooms, and beds.
- Review student applications and approve or reject applications.
- Allocate students to available beds and review hostel occupancy.
- Review student fee-payment submissions and approve or reject them.
- Create and publish a weekly food menu and configure shared meal timings.
- Review food feedback and meal-attendance summaries.
- View room-maintenance complaints, update their status, and add progress or resolution notes.

### Student
- Register and view profile and account status.
- View room, bed, and roommate information when an allocation is active.
- View fee/payment information and submit a payment reference for Admin verification.
- View published food menus, submit meal feedback, and respond to dinner-attendance questions.
- Raise room-maintenance complaints and track their status.

**Payment note:** The payment feature records a transaction/reference ID for Admin review. Do not describe it as a live payment-gateway integration unless a gateway has actually been implemented and tested.

## Technology Stack

- **Language:** Java
- **User interface:** Java Swing
- **Database:** MySQL
- **Connectivity:** JDBC / MySQL Connector/J
- **Build tool:** Maven
- **IDE used during development:** Visual Studio Code

## Project Structure

```text
HostelManagementSystem/
├── database/
│   └── mysql/             # SQL setup and feature scripts
├── src/
│   └── main/
│       └── java/
│           └── com/hostelmanagement/
│               ├── auth/
│               ├── common/
│               ├── dao/
│               ├── model/
│               ├── service/
│               └── ui/
├── pom.xml
├── README.md
└── RUN_HOSTEL_MANAGEMENT.command   # Optional macOS launcher, if present
```

Some files may differ as the project evolves. Use the actual repository tree as the final source of truth.

## Prerequisites

Install and configure:
1. A compatible JDK. The current `pom.xml` specifies Java 21.
2. MySQL Server and MySQL Workbench (Workbench is optional but useful for running SQL scripts).
3. Maven, or the Maven integration available in your development environment.
4. The MySQL Connector/J dependency specified in `pom.xml`.

## Database Setup

1. Start MySQL Server.
2. Open MySQL Workbench and connect to the MySQL instance used by the project.
3. Open the SQL scripts in `database/mysql/` and inspect their intended order and database name.
4. Run the database/schema setup scripts first, followed by any feature-specific scripts that are required by the current version.
5. Confirm that the expected tables exist before launching the Java application.

**Important:** Do not blindly re-run destructive SQL statements against a database containing records. Read each script first and back up existing data before schema changes.

## Database Configuration

The database connection utility may read connection details from environment variables. Check `MySQLConnection.java` in the current source before configuring the application.

If the current version uses the following variables, set them in your shell or launch environment:

```bash
export HOSTEL_DB_URL="jdbc:mysql://localhost:3306/hostel_management"
export HOSTEL_DB_USER="your_mysql_username"
export HOSTEL_DB_PASSWORD="your_mysql_password"
```

Replace the example values with your local configuration. Do not commit real passwords, private keys, or other secrets to GitHub. Keep them in local environment settings and ensure secret files are ignored by Git.

## Run the Application

From the project root—the directory containing `pom.xml`—run:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass=com.hostelmanagement.Main
```

For subsequent runs without cleaning the build, you can normally use:

```bash
mvn exec:java -Dexec.mainClass=com.hostelmanagement.Main
```

If the project includes `RUN_HOSTEL_MANAGEMENT.command`, it can be used as a macOS launcher. Make sure MySQL is running and the database settings are available in the launch environment. Keep the launched Terminal window open while the application is running.

## Suggested Demonstration Flow

1. Sign in as Admin and show the hostel hierarchy and room/bed availability.
2. Review a student application and demonstrate allocation if suitable test data is available.
3. Open fee verification and explain the distinction between pending and approved payments.
4. Publish a weekly menu and show it from a Student account.
5. Submit a food-feedback response and inspect the Admin analytics.
6. Submit a room complaint as Student and update its status as Admin.
7. Verify that changes are reflected in the relevant screens after refresh/reload.

Use test accounts and sample data only. Do not expose real student personal data or credentials during the demonstration.

## Diagrams and Report

The project documentation may include:
- System architecture diagram.
- ER diagrams for hostel/allocation, fees/payments, food/feedback, and complaints.
- An EER diagram for user-role specialization, if its assumptions match the database design.
- The DBMS project report and supporting SQL/query results.

Keep editable `.drawio` source files alongside exported PNG/PDF versions when these are included in the repository. Confirm all keys, foreign keys, and relationship cardinalities against the actual SQL schema before submission.

## Team Contributions

Update this table to reflect the work each member actually completed.

| Team member | Contribution |
|---|---|
| Member 1 — replace with name | Replace with actual contribution |
| Member 2 — replace with name | Replace with actual contribution |
| Member 3 — replace with name | Replace with actual contribution |
| Member 4 — replace with name | Replace with actual contribution |

## Troubleshooting

- **Database connection error:** Check that MySQL is running, the schema exists, and the URL/user/password match the local configuration.
- **Maven build error:** Run `mvn clean compile` from the directory containing `pom.xml` and read the first error reported by Maven.
- **Changes do not appear:** Use the relevant screen's refresh/load control or reopen the screen if that workflow does not refresh automatically.
- **Login or role issue:** Confirm that the test account exists in the database and has the expected status/role.

## Maintainers

- Institution: Symbiosis Institute of Technology, Pune
- Course/project: Java and DBMS Lab — Hostel Management System
- Team members: Replace with actual names
- Faculty guide: Replace if required by the course

---

Before submission, verify all instructions against the final source code and SQL scripts, replace placeholders, and remove any feature description that is not present in the demonstrated build.
