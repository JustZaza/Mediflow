-- ============================================================
-- MediFlow — Hospital & Pharmacy Management System
-- MySQL Schema
-- ============================================================
-- Run with:  mysql -u root -p < schema.sql
-- Requires:  MySQL 8.0+ (uses CHECK constraints, ENUM types)
-- ============================================================

DROP DATABASE IF EXISTS mediflow;
CREATE DATABASE mediflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mediflow;

-- ------------------------------------------------------------
-- 1. USERS  (login accounts for staff of every role)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           ENUM('ADMIN','DOCTOR','RECEPTIONIST','PHARMACIST') NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(100),
    status         ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 2. PATIENTS
-- ------------------------------------------------------------
CREATE TABLE patients (
    patient_id        INT AUTO_INCREMENT PRIMARY KEY,
    patient_code      VARCHAR(20) NOT NULL UNIQUE,        -- human-readable unique ID e.g. P-00001
    first_name        VARCHAR(50) NOT NULL,
    last_name         VARCHAR(50) NOT NULL,
    date_of_birth     DATE NOT NULL,
    gender            ENUM('MALE','FEMALE','OTHER') NOT NULL,
    phone             VARCHAR(20),
    email             VARCHAR(100),
    address           VARCHAR(255),
    emergency_contact_name  VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_patient_dob CHECK (date_of_birth <= CURDATE())
);

-- ------------------------------------------------------------
-- 3. DOCTORS
-- ------------------------------------------------------------
CREATE TABLE doctors (
    doctor_id       INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL UNIQUE,
    full_name       VARCHAR(100) NOT NULL,
    specialization  VARCHAR(100),
    CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 4. APPOINTMENTS
-- ------------------------------------------------------------
CREATE TABLE appointments (
    appointment_id   INT AUTO_INCREMENT PRIMARY KEY,
    patient_id       INT NOT NULL,
    doctor_id        INT NOT NULL,
    appointment_time DATETIME NOT NULL,
    reason           VARCHAR(255),
    status           ENUM('SCHEDULED','CHECKED_IN','IN_PROGRESS','COMPLETED','CANCELLED','NO_SHOW')
                     NOT NULL DEFAULT 'SCHEDULED',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_appt_doctor  FOREIGN KEY (doctor_id)  REFERENCES doctors(doctor_id),
    INDEX idx_appt_doctor_time (doctor_id, appointment_time)
);

-- ------------------------------------------------------------
-- 5. QUEUES  (check-in / priority queue for today's patients)
-- ------------------------------------------------------------
CREATE TABLE queues (
    queue_id        INT AUTO_INCREMENT PRIMARY KEY,
    patient_id      INT NOT NULL,
    appointment_id  INT NOT NULL,
    priority        ENUM('EMERGENCY','HIGH','NORMAL') NOT NULL DEFAULT 'NORMAL',
    queue_number    INT NOT NULL,
    status          ENUM('WAITING','IN_PROGRESS','DONE','SKIPPED') NOT NULL DEFAULT 'WAITING',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_queue_patient     FOREIGN KEY (patient_id)     REFERENCES patients(patient_id),
    CONSTRAINT fk_queue_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id)
);

-- ------------------------------------------------------------
-- 6. MEDICAL RECORDS
-- ------------------------------------------------------------
CREATE TABLE medical_records (
    record_id       INT AUTO_INCREMENT PRIMARY KEY,
    patient_id      INT NOT NULL,
    doctor_id       INT NOT NULL,
    appointment_id  INT NOT NULL,
    symptoms        TEXT,
    diagnosis       TEXT,
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mr_patient     FOREIGN KEY (patient_id)     REFERENCES patients(patient_id),
    CONSTRAINT fk_mr_doctor      FOREIGN KEY (doctor_id)      REFERENCES doctors(doctor_id),
    CONSTRAINT fk_mr_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id)
);

-- ------------------------------------------------------------
-- 7. MEDICINES  (catalog, not stock — stock lives in `inventory`)
-- ------------------------------------------------------------
CREATE TABLE medicines (
    medicine_id   INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    description   VARCHAR(255),
    dosage_form   VARCHAR(50),      -- tablet, syrup, injection...
    strength      VARCHAR(50),      -- e.g. 500mg
    UNIQUE KEY uq_medicine_name_strength (name, strength)
);

-- ------------------------------------------------------------
-- 8. SUPPLIERS
-- ------------------------------------------------------------
CREATE TABLE suppliers (
    supplier_id  INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    contact_name VARCHAR(100),
    phone        VARCHAR(20),
    email        VARCHAR(100),
    address      VARCHAR(255)
);

-- ------------------------------------------------------------
-- 9. INVENTORY  (per-batch stock)
-- ------------------------------------------------------------
CREATE TABLE inventory (
    inventory_id   INT AUTO_INCREMENT PRIMARY KEY,
    medicine_id    INT NOT NULL,
    supplier_id    INT,
    batch_number   VARCHAR(50) NOT NULL,
    quantity       INT NOT NULL DEFAULT 0,
    minimum_stock  INT NOT NULL DEFAULT 10,
    expiry_date    DATE NOT NULL,
    received_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id),
    CONSTRAINT fk_inv_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
    CONSTRAINT chk_inv_quantity CHECK (quantity >= 0),
    UNIQUE KEY uq_inv_medicine_batch (medicine_id, batch_number)
);

-- ------------------------------------------------------------
-- 10. PRESCRIPTIONS
-- ------------------------------------------------------------
CREATE TABLE prescriptions (
    prescription_id   INT AUTO_INCREMENT PRIMARY KEY,
    patient_id        INT NOT NULL,
    doctor_id         INT NOT NULL,
    medical_record_id INT NOT NULL,
    prescribed_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status            ENUM('PENDING','PARTIALLY_DISPENSED','DISPENSED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    CONSTRAINT fk_presc_patient FOREIGN KEY (patient_id)        REFERENCES patients(patient_id),
    CONSTRAINT fk_presc_doctor  FOREIGN KEY (doctor_id)         REFERENCES doctors(doctor_id),
    CONSTRAINT fk_presc_record  FOREIGN KEY (medical_record_id) REFERENCES medical_records(record_id)
);

-- ------------------------------------------------------------
-- 11. PRESCRIPTION ITEMS
-- ------------------------------------------------------------
CREATE TABLE prescription_items (
    item_id          INT AUTO_INCREMENT PRIMARY KEY,
    prescription_id  INT NOT NULL,
    medicine_id      INT NOT NULL,
    dosage           VARCHAR(50),
    frequency        VARCHAR(50),
    duration         VARCHAR(50),
    quantity         INT NOT NULL,
    CONSTRAINT fk_pi_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id) ON DELETE CASCADE,
    CONSTRAINT fk_pi_medicine     FOREIGN KEY (medicine_id)     REFERENCES medicines(medicine_id),
    CONSTRAINT chk_pi_quantity CHECK (quantity > 0)
);

-- ------------------------------------------------------------
-- 12. DISPENSING
-- ------------------------------------------------------------
CREATE TABLE dispensing (
    dispensing_id    INT AUTO_INCREMENT PRIMARY KEY,
    prescription_id  INT NOT NULL,
    pharmacist_id    INT NOT NULL,       -- FK -> users.user_id
    dispensed_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_disp_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id),
    CONSTRAINT fk_disp_pharmacist   FOREIGN KEY (pharmacist_id)   REFERENCES users(user_id)
);

-- ------------------------------------------------------------
-- 13. DISPENSING ITEMS
-- ------------------------------------------------------------
CREATE TABLE dispensing_items (
    dispensing_item_id INT AUTO_INCREMENT PRIMARY KEY,
    dispensing_id       INT NOT NULL,
    medicine_id         INT NOT NULL,
    inventory_id         INT NOT NULL,    -- which batch stock was taken from
    quantity             INT NOT NULL,
    CONSTRAINT fk_di_dispensing FOREIGN KEY (dispensing_id) REFERENCES dispensing(dispensing_id) ON DELETE CASCADE,
    CONSTRAINT fk_di_medicine   FOREIGN KEY (medicine_id)   REFERENCES medicines(medicine_id),
    CONSTRAINT fk_di_inventory  FOREIGN KEY (inventory_id)  REFERENCES inventory(inventory_id),
    CONSTRAINT chk_di_quantity CHECK (quantity > 0)
);

-- ------------------------------------------------------------
-- 14. STOCK MOVEMENTS  (audit trail for every quantity change)
-- ------------------------------------------------------------
CREATE TABLE stock_movements (
    movement_id   INT AUTO_INCREMENT PRIMARY KEY,
    inventory_id  INT NOT NULL,
    movement_type ENUM('RECEIVE','DISPENSE','ADJUSTMENT','EXPIRED_REMOVAL') NOT NULL,
    quantity      INT NOT NULL,             -- positive = added, negative = removed
    reference     VARCHAR(100),             -- e.g. "dispensing_id=42" or "import batch 2026-09"
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sm_inventory FOREIGN KEY (inventory_id) REFERENCES inventory(inventory_id)
);

-- ------------------------------------------------------------
-- Helpful views for dashboards / reports
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW v_low_stock AS
SELECT i.inventory_id, m.name AS medicine_name, i.batch_number, i.quantity, i.minimum_stock, i.expiry_date
FROM inventory i
JOIN medicines m ON m.medicine_id = i.medicine_id
WHERE i.quantity <= i.minimum_stock;

CREATE OR REPLACE VIEW v_expiring_soon AS
SELECT i.inventory_id, m.name AS medicine_name, i.batch_number, i.quantity, i.expiry_date
FROM inventory i
JOIN medicines m ON m.medicine_id = i.medicine_id
WHERE i.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY);

CREATE OR REPLACE VIEW v_todays_queue AS
SELECT q.queue_id, q.queue_number, q.priority, q.status,
       CONCAT(p.first_name, ' ', p.last_name) AS patient_name,
       a.appointment_time, d.full_name AS doctor_name
FROM queues q
JOIN patients p       ON p.patient_id = q.patient_id
JOIN appointments a   ON a.appointment_id = q.appointment_id
JOIN doctors d        ON d.doctor_id = a.doctor_id
WHERE DATE(a.appointment_time) = CURDATE()
ORDER BY FIELD(q.priority, 'EMERGENCY','HIGH','NORMAL'), q.queue_number;
