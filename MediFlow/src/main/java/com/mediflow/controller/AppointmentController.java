package com.mediflow.controller;

import com.mediflow.model.Appointment;
import com.mediflow.service.AppointmentService;

import java.time.LocalDateTime;
import java.util.List;

public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) { this.appointmentService = appointmentService; }

    public Appointment book(int patientId, int doctorId, LocalDateTime time, String reason) {
        return appointmentService.book(patientId, doctorId, time, reason);
    }
    public void cancel(int appointmentId) { appointmentService.cancel(appointmentId); }
    public void checkIn(int appointmentId) { appointmentService.checkIn(appointmentId); }
    public List<Appointment> todaysAppointments(int doctorId) { return appointmentService.forDoctorToday(doctorId); }
}
