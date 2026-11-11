package com.mediflow.service;

import com.mediflow.dao.AppointmentDAO;
import com.mediflow.exception.InvalidAppointmentException;
import com.mediflow.model.Appointment;
import com.mediflow.util.ValidationUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** FR-03: create/cancel/reschedule appointments. Enforces "no overlapping appointments per doctor". */
@Service
public class AppointmentService {
    private final AppointmentDAO appointmentDAO;

    public AppointmentService(AppointmentDAO appointmentDAO) { this.appointmentDAO = appointmentDAO; }

    public Appointment book(int patientId, int doctorId, LocalDateTime time, String reason) {
        ValidationUtil.requirePositive(patientId, "Patient");
        ValidationUtil.requirePositive(doctorId, "Doctor");
        if (time == null || time.isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new InvalidAppointmentException("Appointment time must be in the future");
        }
        if (appointmentDAO.hasConflict(doctorId, time)) {
            throw new InvalidAppointmentException("This doctor already has an appointment at that time");
        }
        Appointment appt = new Appointment(patientId, doctorId, time, reason);
        return appointmentDAO.insert(appt);
    }

    public void cancel(int appointmentId) {
        Appointment appt = requireExisting(appointmentId);
        if (appt.getStatus() == Appointment.Status.CHECKED_IN || appt.getStatus() == Appointment.Status.IN_PROGRESS) {
            throw new InvalidAppointmentException("Cannot cancel an appointment that is already checked in");
        }
        appointmentDAO.updateStatus(appointmentId, Appointment.Status.CANCELLED);
    }

    /** Business rule: cancelled appointments cannot be checked in. */
    public void checkIn(int appointmentId) {
        Appointment appt = requireExisting(appointmentId);
        if (appt.getStatus() == Appointment.Status.CANCELLED) {
            throw new InvalidAppointmentException("Cannot check in a cancelled appointment");
        }
        appointmentDAO.updateStatus(appointmentId, Appointment.Status.CHECKED_IN);
    }

    public void markStatus(int appointmentId, Appointment.Status status) {
        requireExisting(appointmentId);
        appointmentDAO.updateStatus(appointmentId, status);
    }

    public List<Appointment> forDoctorToday(int doctorId) {
        return appointmentDAO.findByDoctorAndDate(doctorId, java.time.LocalDate.now());
    }

    private Appointment requireExisting(int appointmentId) {
        Optional<Appointment> appt = appointmentDAO.findById(appointmentId);
        if (appt.isEmpty()) throw new InvalidAppointmentException("Appointment not found: " + appointmentId);
        return appt.get();
    }

    public Appointment reschedule(int appointmentId, LocalDateTime time, String reason) {
        Appointment appt = requireExisting(appointmentId);
        if (appt.getStatus() != Appointment.Status.SCHEDULED) {
            throw new InvalidAppointmentException("Only scheduled appointments can be changed");
        }
        // Only insist on a future time if the time is actually being changed, so the reason
        // of an appointment earlier today can still be edited. The doctor stays the same.
        boolean timeChanged = !time.equals(appt.getAppointmentTime());
        if (timeChanged && time.isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new InvalidAppointmentException("Appointment time must be in the future");
        }
        if (appointmentDAO.hasConflictExcluding(appt.getDoctorId(), time, appointmentId)) {
            throw new InvalidAppointmentException("This doctor already has an appointment at that time");
        }
        appointmentDAO.update(appointmentId, time, reason);
        return requireExisting(appointmentId);
    }

    public void delete(int appointmentId) {
        Appointment appt = requireExisting(appointmentId);
        if (appt.getStatus() != Appointment.Status.CANCELLED) {
            throw new InvalidAppointmentException("Only cancelled appointments can be deleted. Cancel it first.");
        }
        appointmentDAO.delete(appointmentId);
    }
}
