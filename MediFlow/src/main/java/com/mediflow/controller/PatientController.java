package com.mediflow.controller;

import com.mediflow.model.Patient;
import com.mediflow.service.PatientService;

import java.util.List;
import java.util.Optional;

public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService) { this.patientService = patientService; }

    public Patient registerPatient(Patient patient) { return patientService.register(patient); }
    public List<Patient> searchPatients(String query) { return patientService.search(query); }
    public Optional<Patient> getPatient(int patientId) { return patientService.findById(patientId); }
    public void updatePatient(Patient patient) { patientService.update(patient); }
}
