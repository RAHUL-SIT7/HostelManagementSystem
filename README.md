# Hostel Management System

A Java desktop application for managing hostel accommodation and day-to-day operations. The user interface is built with Java Swing, and the application connects to MySQL through JDBC.

## Features

### Administrator

- Manage hostels, blocks, floors, room types, rooms, and beds.
- Review student applications and approve or reject them.
- View hostel availability and occupancy, and allocate beds to approved students.
- Review student-submitted fee-payment references and approve or reject them.
- Publish the weekly food menu and configure meal timings.
- Review food feedback and meal-attendance summaries.
- Review room-maintenance complaints, update their status, and add resolution notes.

### Student

- Register for an account and view account status and profile details.
- View assigned hostel, room, bed, and roommate information.
- View fee information and submit a payment reference for administrator review.
- View the published food menu, submit meal feedback, and respond to meal-attendance questions.
- Submit a room-maintenance complaint and view its status.

> Payment records contain a transaction or reference ID for administrator review. The application does not process payments through a payment gateway.

## Technology

- **Language:** Java 21
- **Desktop user interface:** Java Swing
- **Database:** MySQL
- **Database connectivity:** JDBC with MySQL Connector/J
- **Build tool:** Maven

This is the Java desktop application. Its user interface runs as a desktop program; it is not an HTML/CSS/JavaScript website and does not use a Node.js server.

## Project structure

```text
HostelManagementSystem/
├── database/
│   └── mysql/                 # MySQL setup and feature SQL files
├── src/main/java/com/hostelmanagement/
│   ├── auth/                  # Login and password utilities
│   ├── common/database/       # MySQL JDBC connection
│   ├── dao/                   # Database access
│   ├── model/                 # Data models
│   ├── service/               # Application operations
│   └── ui/                    # Swing windows and forms
├── pom.xml
└── README.md
```

## Requirements

Install or configure:

1. JDK 21.
2. MySQL Server.
3. Maven, or Maven support in your IDE.
4. MySQL Connector/J, which Maven downloads from the dependency in `pom.xml`.

MySQL Workbench is optional, but can be used to inspect and run the SQL files.

## Database setup

1. Start MySQL Server.
2. Open the SQL files under `database/mysql/` and review their contents and order.
3. Create the `hostel_management` database and run the schema setup scripts, followed by any feature scripts required by the current source version.
4. Confirm the tables exist before starting the Java application.

The scripts include schema changes and feature-specific SQL. Review them before running, especially if the database already contains data. Back up existing data before applying schema changes; do not blindly rerun `ALTER TABLE` statements.

## Database configuration

The JDBC connection utility reads these environment variables:

- `HOSTEL_DB_URL` — defaults to `jdbc:mysql://127.0.0.1:3306/hostel_management` with local connection options.
- `HOSTEL_DB_USER` — defaults to `hostel_app`.
- `HOSTEL_DB_PASSWORD` — required; the application reports an error if it is not set.

Set them in the environment used to launch the application. For example, in PowerShell:

```powershell
$env:HOSTEL_DB_URL = "jdbc:mysql://127.0.0.1:3306/hostel_management?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000"
$env:HOSTEL_DB_USER = "your_mysql_username"
$env:HOSTEL_DB_PASSWORD = "your_mysql_password"
```

Use your own MySQL credentials. Do not commit real passwords or other secrets to Git.

## Run the application

From the project root—the folder containing `pom.xml`—run:

```powershell
mvn clean compile
mvn exec:java "-Dexec.mainClass=com.hostelmanagement.Main"
```

The application opens the Swing login window. MySQL must be running, and the database settings must be available in the same environment.

## Suggested demonstration

1. Sign in with an administrator account and review the hostel and room setup.
2. Review a student application and approve it.
3. Allocate an available bed and check the occupancy view.
4. Submit a payment reference as a student and review it as an administrator.
5. Publish a weekly menu and view it from the student account.
6. Submit food feedback and meal attendance, then review the administrator summaries.
7. Submit a maintenance complaint and update its status as an administrator.

Use sample accounts and test data for demonstrations. Do not expose real student personal information or passwords.

## Troubleshooting

- **MySQL connection error:** Confirm MySQL is running, the `hostel_management` database and tables exist, and the environment variables match your MySQL setup.
- **Missing password error:** Set `HOSTEL_DB_PASSWORD` in the environment used to launch the application.
- **Maven build error:** Run Maven from the directory containing `pom.xml` and review the first reported error.
- **Changes do not appear:** Refresh the relevant screen or reopen it if the screen does not reload automatically.
- **Login or role issue:** Confirm the account exists in MySQL and has the expected role and account status.


