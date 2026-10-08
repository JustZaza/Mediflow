# 🏥 MediFlow: Hospital and Pharmacy Management System

MediFlow is a multi-module hospital and pharmacy management system built in **Java** with a **MySQL** database. It models the path from patient intake through to stock-impacting pharmacy transactions, and enforces business rules in code so that records stay consistent, for example by preventing negative stock and double-bookings.

> 🚧 **Status:** core system complete. A Spring Boot REST API layer is currently in progress.

---

## ✨ Features

- 🧑‍⚕️ **Patient intake:** register patients and manage their records.
- 📅 **Appointments:** schedule appointments with double-booking prevention.
- 💊 **Pharmacy and stock:** manage medicine stock levels.
- ⚙️ **Transactional dispensing engine:** dispensing is processed as a single transaction, so a failed step does not leave stock or records half-updated.
- 🛡️ **Business-rule validation:** data-integrity checks, including rejecting any dispense that would take stock below zero.
- 📚 **Documentation and demo:** functional requirements, database schema, a structured test plan, and an interactive demo that walks through the workflow.

---

## 🛠️ Tech Stack

| Area | Technology |
| --- | --- |
| ☕ Language | Java |
| 🗄️ Database | MySQL (SQL) |
| 🌱 API layer (in progress) | Spring Boot (REST) |
| 📦 Build tool | [Maven] |
| 🧪 Testing | [Postman] |

---

## 📏 Business Rules

These are enforced in the application and database logic:

1. 🚫 Stock can never go negative. A dispense that exceeds available stock is rejected.
2. 🔒 A resource cannot be double-booked for the same time slot.
3. 🔁 Dispensing updates all affected records in one transaction. If any step fails, the whole transaction is rolled back.


---

## 🚀 Getting Started

### 📋 Prerequisites

- Java 17
- MySQL
- Maven
- IntelliJ IDEA

### ⚙️ Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/JustZaza/Mediflow.git
   cd Mediflow
   ```

2. Create the database and import the schema:

   ```bash
   mysql -u root -p -e "CREATE DATABASE mediflow;"
   mysql -u root -p mediflow < [path/to/schema.sql]
   ```

3. Set your database credentials in [config file or class]:

   ```
   url      = jdbc:mysql://localhost:3306/mediflow
   username = [your_username]
   password = [your_password]
   ```

   🔐 Do not commit real credentials to the repository.

4. Run the application:

   ```bash
   [command, or "Run Main.java from your IDE"]
   ```

---

## 🌐 REST API (in progress)

The Spring Boot layer will expose the existing logic over REST. Endpoints will be listed here as they are completed.

| Method | Endpoint | Description |
| --- | --- | --- |
| [GET] | [/api/patients] | [List patients] |
| [POST] | [/api/dispense] | [Dispense stock, rejected if stock would go negative] |

---

## 🧪 Testing

- ✅ A structured test plan covers the main workflows and the business rules above. See [path/to/test-plan].
- 🔜 [Will be added when done: JUnit tests for the business rules, and a Postman collection for the API endpoints in /postman]

---

## 📖 Documentation

- 📝 Functional requirements: [path or link]
- 🗂️ Database schema: [path or link, ERD image if you have one]
- 🧾 Test plan: [path or link]
- 🎬 Interactive demo: [path or link]

---

## 📁 Project Structure

```
mediflow/
├── src/
    └── main
      └── java
        └── com.mediflow
          ├── controller
          ├── dao
          ├── database
          ├── demo
          ├── exception
          ├── model
          ├── repository
          ├── service
          └── util
├── database/
│   └── schema.sql
├── docs/
└── README.md
```

---

## 🗺️ Roadmap

- [x] Core modules and transactional dispensing engine
- [x] Business-rule validation
- [ ] Spring Boot REST API
- [ ] Automated tests (JUnit)
- [ ] Postman collection
- [ ] Dockerfile

---

## 👩‍💻 Author

**Azande Chamu**

- 🔗 LinkedIn: [linkedin.com/in/azandechamu](https://linkedin.com/in/azandechamu)
- 💻 GitHub: [github.com/JustZaza](https://github.com/JustZaza)
