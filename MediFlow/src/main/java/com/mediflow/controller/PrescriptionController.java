package com.mediflow.controller;

import com.mediflow.model.Prescription;
import com.mediflow.model.PrescriptionItem;
import com.mediflow.model.User;
import com.mediflow.service.PrescriptionService;

import java.util.List;

public class PrescriptionController {
    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) { this.prescriptionService = prescriptionService; }

    public Prescription create(User doctor, int patientId, int doctorId, int medicalRecordId, List<PrescriptionItem> items) {
        return prescriptionService.create(doctor, patientId, doctorId, medicalRecordId, items);
    }
    public List<Prescription> pending() { return prescriptionService.pending(); }
}
