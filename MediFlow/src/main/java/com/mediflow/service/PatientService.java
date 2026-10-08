package com.mediflow.service;

import com.mediflow.dao.PatientDAO;
import com.mediflow.exception.DuplicatePatientException;
import com.mediflow.model.Patient;
import com.mediflow.util.ValidationUtil;

import java.util.List;
import java.util.Optional;

/** FR-02: register, search, view, update patients. Enforces unique patient ID and DOB rules. */
public class PatientService {
    private final PatientDAO patientDAO;

    public PatientService(PatientDAO patientDAO) { this.patientDAO = patientDAO; }

    public Patient register(Patient patient) {
        ValidationUtil.requireNonBlank(patient.getPatientCode(), "Patient code");
        ValidationUtil.requireNonBlank(patient.getFirstName(), "First name");
        ValidationUtil.requireNonBlank(patient.getLastName(), "Last name");
        ValidationUtil.requireDobNotInFuture(patient.getDateOfBirth());

        if (patientDAO.existsByCode(patient.getPatientCode())) {
            throw new DuplicatePatientException("A patient with ID " + patient.getPatientCode() + " already exists");
        }
        return patientDAO.insert(patient);
    }

    public List<Patient> search(String nameFragment) {
        ValidationUtil.requireNonBlank(nameFragment, "Search text");
        return patientDAO.search(nameFragment);
    }

    public Optional<Patient> findById(int patientId) {
        return patientDAO.findById(patientId);
    }

    public void update(Patient patient) {
        ValidationUtil.requireNonBlank(patient.getFirstName(), "First name");
        ValidationUtil.requireNonBlank(patient.getLastName(), "Last name");
        patientDAO.update(patient);
    }
}
