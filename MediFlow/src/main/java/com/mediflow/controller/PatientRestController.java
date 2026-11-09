package com.mediflow.controller;

import com.mediflow.dao.PatientDAO;
import com.mediflow.model.Patient;
import com.mediflow.service.PatientService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientRestController {
    private final PatientDAO patientDAO;
    private final PatientService patientService;

    public PatientRestController(PatientDAO patientDAO, PatientService patientService) {
        this.patientDAO = patientDAO;
        this.patientService = patientService;
    }

    @GetMapping
    public List<Patient> all() {
        return patientDAO.search("");   // empty text matches everyone
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Patient register(@RequestBody Patient patient) {
        int next = patientDAO.search("").stream().mapToInt(Patient::getPatientId).max().orElse(0) + 1;
        patient.setPatientCode(String.format("P-%05d", next));   // the page no longer invents codes
        return patientService.register(patient);
    }

    @PutMapping("/{id}")
    public Patient update(@PathVariable int id, @RequestBody Patient patient) {
        patient.setPatientId(id);
        patientService.update(patient);   // validates first/last name, updates contact fields
        return patientService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        patientService.delete(id);
    }
}