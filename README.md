# 🏠 Home Automation System GUI (Java/JDBC)

A robust, GUI-based home automation management system developed in Java. It utilizes the Data Access Object (DAO) pattern with JDBC to manage user accounts and control various smart home devices via a MySQL database. This project was built to demonstrate key object-oriented programming (OOP) principles, multithreading, and database integration.

***

## ✨ Key Features

* **User Management:** Admin users can perform **CRUD** (Create, Read, Update, Delete) operations on all system users (Admins and Homeowners).
* **Polymorphic Device Control:** Homeowners can switch devices (e.g., **Light**, **Gate**) ON/OFF or adjust settings (e.g., brightness slider for Light).
* **Background Monitoring:** A dedicated **Multithreaded** system runs in the background to simulate monitoring and reporting on environment status (security, temperature).
* **Role-Based Dashboards:** Separate user interfaces for **Admin** and **Homeowner**, demonstrating **Inheritance** and **Polymorphism**.
* **JDBC Integration:** Full implementation of the DAO pattern using `Connection`, `PreparedStatement`, and `ResultSet` for secure and efficient database operations.

***

## ⚙️ Technologies Used

* **Language:** Java 17+
* **GUI Framework:** Swing
* **Database:** MySQL (JDBC)
* **Driver:** MySQL Connector/J 9.5.0

***

## 🚀 Getting Started

### Prerequisites

Before running the application, you must have the following installed and configured:

1.  **Java Development Kit (JDK) 17+**
2.  **MySQL Server**
3.  **MySQL Connector/J JAR File:** Ensure the `mysql-connector-j-9.5.0.jar` file is in your project's library path.
4.  **Database Configuration:** Update the connection details in the `DBConnectionManager` class (around line 135) of `HomeAutomationGUI.java` with your actual credentials.

### Database Setup

The application requires two tables in your MySQL database (`home_automation_db` is assumed). Run the following SQL commands to create the necessary tables:

```sql
-- 1. Create the users table
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL -- 'Admin' or 'Homeowner'
);

-- 2. Create the devices table
CREATE TABLE devices (
    device_id INT AUTO_INCREMENT PRIMARY KEY,
    homeowner_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    device_type VARCHAR(50) NOT NULL, -- e.g., 'Light', 'Gate'
    status VARCHAR(50) NOT NULL, -- e.g., 'ON', 'OFF', 'OPEN', 'CLOSED'
    setting_value INT DEFAULT 0,
    FOREIGN KEY (homeowner_id) REFERENCES users(email) ON DELETE CASCADE
);
