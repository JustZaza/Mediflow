-- ============================================================
-- MediFlow — Seed Data
-- Run after schema.sql:  mysql -u root -p mediflow < seed.sql
-- Note: password_hash values below are placeholder SHA-256 hashes
-- of "password123" — replace via the app's registration flow in
-- a real deployment. Never keep demo credentials in production.
-- ============================================================
USE mediflow;

-- Users (1 admin, 2 doctors, 1 receptionist, 1 pharmacist)
INSERT INTO users (username, password_hash, role, full_name, email) VALUES
('admin',      '$2a$10$demoHashAdmin000000000000000000000000000000000000000', 'ADMIN',        'System Admin',     'admin@mediflow.local'),
('dr.smith',   '$2a$10$demoHashDoctor10000000000000000000000000000000000000', 'DOCTOR',        'Dr. Alice Smith',  'a.smith@mediflow.local'),
('dr.jones',   '$2a$10$demoHashDoctor20000000000000000000000000000000000000', 'DOCTOR',        'Dr. Brian Jones',  'b.jones@mediflow.local'),
('reception1', '$2a$10$demoHashReception000000000000000000000000000000000000', 'RECEPTIONIST', 'Carol Reyes',      'c.reyes@mediflow.local'),
('pharm1',     '$2a$10$demoHashPharmacist00000000000000000000000000000000000', 'PHARMACIST',   'David Okoye',      'd.okoye@mediflow.local');

INSERT INTO doctors (user_id, full_name, specialization) VALUES
((SELECT user_id FROM users WHERE username='dr.smith'), 'Dr. Alice Smith', 'General Practice'),
((SELECT user_id FROM users WHERE username='dr.jones'), 'Dr. Brian Jones', 'Pediatrics');

-- Patients
INSERT INTO patients (patient_code, first_name, last_name, date_of_birth, gender, phone, email, address, emergency_contact_name, emergency_contact_phone) VALUES
('P-00001', 'Thabo',  'Nkosi',    '1988-04-12', 'MALE',   '0821234567', 'thabo.n@example.com',  '12 Main Rd, Johannesburg', 'Lindiwe Nkosi', '0827654321'),
('P-00002', 'Sarah',  'Adams',    '1995-11-02', 'FEMALE', '0731234567', 'sarah.a@example.com',  '4 Oak Ave, Sandton',       'Peter Adams',   '0739876543'),
('P-00003', 'Kagiso', 'Molefe',   '1972-06-30', 'MALE',   '0611234567', 'kagiso.m@example.com', '78 Church St, Pretoria',   'Naledi Molefe', '0619876543');

-- Medicines
INSERT INTO medicines (name, description, dosage_form, strength) VALUES
('Amoxicillin', 'Broad-spectrum antibiotic', 'Capsule', '500mg'),
('Paracetamol', 'Pain / fever relief',        'Tablet',  '500mg'),
('Ibuprofen',   'NSAID pain reliever',        'Tablet',  '200mg'),
('Amlodipine',  'Blood pressure management',  'Tablet',  '5mg');

-- Suppliers
INSERT INTO suppliers (name, contact_name, phone, email, address) VALUES
('PharmaCorp Distributors', 'Nomvula Dlamini', '0110001111', 'orders@pharmacorp.example', '1 Industrial Rd, Midrand'),
('MedSupply SA',            'James Botha',     '0110002222', 'sales@medsupplysa.example', '9 Depot St, Germiston');

-- Inventory (batches)
INSERT INTO inventory (medicine_id, supplier_id, batch_number, quantity, minimum_stock, expiry_date) VALUES
((SELECT medicine_id FROM medicines WHERE name='Amoxicillin'), 1, 'AMX-2026-01', 120, 20, '2027-03-01'),
((SELECT medicine_id FROM medicines WHERE name='Paracetamol'), 1, 'PCM-2026-01', 8,   20, '2026-12-01'),  -- low stock example
((SELECT medicine_id FROM medicines WHERE name='Ibuprofen'),   2, 'IBU-2025-05', 45,  15, '2026-10-01'),  -- expiring soon example
((SELECT medicine_id FROM medicines WHERE name='Amlodipine'),  2, 'AML-2026-02', 60,  10, '2027-06-01');

-- Appointments (today, for demo)
INSERT INTO appointments (patient_id, doctor_id, appointment_time, reason, status) VALUES
(1, 1, CONCAT(CURDATE(), ' 09:00:00'), 'Persistent cough',        'SCHEDULED'),
(2, 1, CONCAT(CURDATE(), ' 09:30:00'), 'Follow-up blood pressure','SCHEDULED'),
(3, 2, CONCAT(CURDATE(), ' 10:00:00'), 'Routine checkup',         'SCHEDULED');
