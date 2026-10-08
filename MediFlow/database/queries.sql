-- ============================================================
-- MediFlow — Sample Operational Queries
-- ============================================================
USE mediflow;

-- 1. Today's queue, ordered by priority then queue number
SELECT * FROM v_todays_queue;

-- 2. Low stock medicines (quantity <= minimum_stock)
SELECT * FROM v_low_stock;

-- 3. Medicines expiring within 30 days
SELECT * FROM v_expiring_soon;

-- 4. Prescriptions awaiting dispensing
SELECT pr.prescription_id, CONCAT(p.first_name,' ',p.last_name) AS patient,
       d.full_name AS doctor, pr.prescribed_at, pr.status
FROM prescriptions pr
JOIN patients p ON p.patient_id = pr.patient_id
JOIN doctors d  ON d.doctor_id  = pr.doctor_id
WHERE pr.status IN ('PENDING','PARTIALLY_DISPENSED')
ORDER BY pr.prescribed_at;

-- 5. A doctor's appointments for today
SELECT a.appointment_id, a.appointment_time, a.status,
       CONCAT(p.first_name,' ',p.last_name) AS patient
FROM appointments a
JOIN patients p ON p.patient_id = a.patient_id
WHERE a.doctor_id = 1 AND DATE(a.appointment_time) = CURDATE()
ORDER BY a.appointment_time;

-- 6. Check for overlapping appointment before booking (business rule FR-03/6)
--    Replace :doctor_id / :start / :end with real params in application code.
SELECT COUNT(*) AS conflict_count
FROM appointments
WHERE doctor_id = :doctor_id
  AND status NOT IN ('CANCELLED','NO_SHOW')
  AND appointment_time = :start;

-- 7. Full patient medical history
SELECT mr.created_at, mr.symptoms, mr.diagnosis, mr.notes, d.full_name AS doctor
FROM medical_records mr
JOIN doctors d ON d.doctor_id = mr.doctor_id
WHERE mr.patient_id = :patient_id
ORDER BY mr.created_at DESC;

-- 8. Dispensing activity report (for Excel export, FR-09/FR-10)
SELECT dp.dispensing_id, dp.dispensed_at, u.full_name AS pharmacist,
       m.name AS medicine, di.quantity, pr.prescription_id
FROM dispensing dp
JOIN dispensing_items di ON di.dispensing_id = dp.dispensing_id
JOIN medicines m         ON m.medicine_id   = di.medicine_id
JOIN users u              ON u.user_id      = dp.pharmacist_id
JOIN prescriptions pr     ON pr.prescription_id = dp.prescription_id
ORDER BY dp.dispensed_at DESC;

-- 9. Stock movement audit trail for one medicine
SELECT sm.created_at, sm.movement_type, sm.quantity, sm.reference
FROM stock_movements sm
JOIN inventory i ON i.inventory_id = sm.inventory_id
WHERE i.medicine_id = :medicine_id
ORDER BY sm.created_at DESC;

-- 10. Dashboard summary counts (pharmacy)
SELECT
  (SELECT COUNT(*) FROM medicines) AS total_medicines,
  (SELECT COUNT(*) FROM v_low_stock) AS low_stock_count,
  (SELECT COUNT(*) FROM inventory WHERE expiry_date < CURDATE()) AS expired_count,
  (SELECT COUNT(*) FROM v_expiring_soon) AS expiring_soon_count,
  (SELECT COUNT(*) FROM prescriptions WHERE status IN ('PENDING','PARTIALLY_DISPENSED')) AS pending_prescriptions;
