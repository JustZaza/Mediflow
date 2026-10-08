package com.mediflow.service;

import com.mediflow.dao.PrescriptionDAO;
import com.mediflow.exception.InvalidPrescriptionException;
import com.mediflow.model.Prescription;
import com.mediflow.model.PrescriptionItem;
import com.mediflow.model.User;

import java.util.List;
import java.util.Optional;

/**
 * FR-06: doctors create prescriptions with one or more items.
 * Business rules enforced here: at least one item, and only DOCTOR-role users may prescribe.
 */
public class PrescriptionService {
    private final PrescriptionDAO prescriptionDAO;

    public PrescriptionService(PrescriptionDAO prescriptionDAO) { this.prescriptionDAO = prescriptionDAO; }

    public Prescription create(User prescriber, int patientId, int doctorId, int medicalRecordId, List<PrescriptionItem> items) {
        if (prescriber.getRole() != User.Role.DOCTOR) {
            throw new InvalidPrescriptionException("Only doctors can create prescriptions");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidPrescriptionException("A prescription must contain at least one medicine item");
        }
        for (PrescriptionItem item : items) {
            if (item.getQuantity() <= 0) {
                throw new InvalidPrescriptionException("Prescription item quantity must be greater than zero");
            }
        }
        Prescription prescription = new Prescription(patientId, doctorId, medicalRecordId);
        items.forEach(prescription::addItem);
        return prescriptionDAO.insert(prescription);
    }

    public Optional<Prescription> findById(int prescriptionId) {
        return prescriptionDAO.findById(prescriptionId);
    }

    public List<Prescription> pending() {
        return prescriptionDAO.findPending();
    }
}
