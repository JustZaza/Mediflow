package com.mediflow.service;

import com.mediflow.dao.DoctorDAO;
import com.mediflow.dao.MedicalRecordDAO;
import com.mediflow.dao.PatientDAO;
import com.mediflow.dao.PrescriptionDAO;
import com.mediflow.exception.NotificationException;
import com.mediflow.model.*;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final MedicalRecordDAO medicalRecordDAO;
    private final PrescriptionDAO prescriptionDAO;
    private final PatientDAO patientDAO;
    private final DoctorDAO doctorDAO;
    private final String from;

    public NotificationService(JavaMailSender mailSender, MedicalRecordDAO medicalRecordDAO,
                               PrescriptionDAO prescriptionDAO, PatientDAO patientDAO, DoctorDAO doctorDAO,
                               @Value("${mediflow.mail.from:mediflow@example.com}") String from) {
        this.mailSender = mailSender;
        this.medicalRecordDAO = medicalRecordDAO;
        this.prescriptionDAO = prescriptionDAO;
        this.patientDAO = patientDAO;
        this.doctorDAO = doctorDAO;
        this.from = from;
    }

    /** Emails the consultation notes and any prescription to the patient. Returns the address used. */
    public String emailConsultation(int recordId, User sender) {
        MedicalRecord record = medicalRecordDAO.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Consultation record not found"));
        Patient patient = patientDAO.findById(record.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        String to = patient.getEmail();
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException(patient.getFirstName() + " " + patient.getLastName()
                    + " has no email address on file.");
        }
        Prescription rx = prescriptionDAO.findByMedicalRecordId(recordId).orElse(null);
        String doctorName = doctorDAO.findAll().stream()
                .filter(d -> d.getDoctorId() == record.getDoctorId())
                .map(Doctor::getFullName).findFirst().orElse("your doctor");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to.trim());
            helper.setSubject("Your consultation summary from MediFlow");
            helper.setText(buildHtml(patient, doctorName, record, rx), true);
            mailSender.send(message);
        } catch (MailException | jakarta.mail.MessagingException e) {
            log.error("Email failed for record {}: {}", recordId, e.getMessage(), e);   // <-- add this
            throw new NotificationException(
                    "The email could not be sent. Check the mail settings and the patient's address.", e);
        }
        // Audit trail: who sent what, to whom. The medical content itself is never logged.
        log.info("Consultation email sent: recordId={} by userId={} to={}", recordId, sender.getUserId(), mask(to));
        return to;
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }

    private static String h(String s) {
        return s == null || s.isBlank() ? "—" : HtmlUtils.htmlEscape(s).replace("\n", "<br>");
    }

    private String buildHtml(Patient patient, String doctorName, MedicalRecord rec, Prescription rx) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial,sans-serif;max-width:600px;color:#1d2b27\">")
                .append("<h2 style=\"color:#0f766e\">MediFlow — consultation summary</h2>")
                .append("<p>Dear ").append(h(patient.getFirstName())).append(",</p>")
                .append("<p>Here is a summary of your visit with ").append(h(doctorName)).append(".</p>")
                .append("<h3>Consultation</h3>")
                .append("<p><b>Symptoms:</b> ").append(h(rec.getSymptoms())).append("</p>")
                .append("<p><b>Diagnosis:</b> ").append(h(rec.getDiagnosis())).append("</p>")
                .append("<p><b>Doctor's notes:</b> ").append(h(rec.getNotes())).append("</p>");

        if (rx != null && !rx.getItems().isEmpty()) {
            sb.append("<h3>Prescription</h3>")
                    .append("<table style=\"border-collapse:collapse;width:100%\">")
                    .append("<tr style=\"background:#e6f4f1\"><th align=\"left\">Medicine</th><th align=\"left\">Dosage</th>")
                    .append("<th align=\"left\">How often</th><th align=\"left\">Quantity</th></tr>");
            for (PrescriptionItem it : rx.getItems()) {
                sb.append("<tr><td style=\"border-top:1px solid #ddd\">").append(h(it.getMedicineName()))
                        .append("</td><td style=\"border-top:1px solid #ddd\">").append(h(it.getDosage()))
                        .append("</td><td style=\"border-top:1px solid #ddd\">").append(h(it.getFrequency()))
                        .append("</td><td style=\"border-top:1px solid #ddd\">").append(it.getQuantity())
                        .append("</td></tr>");
            }
            sb.append("</table><p>Collect your medicine from the MediFlow pharmacy.</p>");
        } else {
            sb.append("<p>No medicine was prescribed at this visit.</p>");
        }
        sb.append("<hr><p style=\"font-size:12px;color:#667\">This message contains confidential medical information ")
                .append("intended only for ").append(h(patient.getFirstName())).append(". ")
                .append("If it reached you by mistake, please delete it and tell the clinic. ")
                .append("Do not reply to this email; contact the clinic for questions.</p></div>");
        return sb.toString();
    }
}