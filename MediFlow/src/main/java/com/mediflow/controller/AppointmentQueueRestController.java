package com.mediflow.controller;

import com.mediflow.dao.AppointmentDAO;
import com.mediflow.dao.DoctorDAO;
import com.mediflow.dao.QueueDAO;
import com.mediflow.model.Appointment;
import com.mediflow.model.Doctor;
import com.mediflow.model.QueueEntry;
import com.mediflow.service.AppointmentService;
import com.mediflow.service.QueueService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AppointmentQueueRestController {
    private final DoctorDAO doctorDAO;
    private final AppointmentDAO appointmentDAO;
    private final QueueDAO queueDAO;
    private final AppointmentService appointmentService;
    private final QueueService queueService;

    public AppointmentQueueRestController(DoctorDAO doctorDAO, AppointmentDAO appointmentDAO, QueueDAO queueDAO,
                                          AppointmentService appointmentService, QueueService queueService) {
        this.doctorDAO = doctorDAO;
        this.appointmentDAO = appointmentDAO;
        this.queueDAO = queueDAO;
        this.appointmentService = appointmentService;
        this.queueService = queueService;
    }

    record BookRequest(int patientId, int doctorId, LocalDateTime appointmentTime, String reason) {}
    record CheckInRequest(int appointmentId, int patientId, QueueEntry.Priority priority) {}

    @GetMapping("/doctors")
    public List<Doctor> doctors() { return doctorDAO.findAll(); }

    @GetMapping("/appointments")
    public List<Appointment> appointments(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentDAO.findByDate(date);
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public Appointment book(@RequestBody BookRequest r) {
        return appointmentService.book(r.patientId(), r.doctorId(), r.appointmentTime(), r.reason());
    }

    @PostMapping("/appointments/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable int id) { appointmentService.cancel(id); }

    @GetMapping("/queue")
    public List<QueueEntry> queue() { return queueDAO.findToday(); }

    @PostMapping("/queue/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueEntry checkIn(@RequestBody CheckInRequest r) {
        appointmentService.checkIn(r.appointmentId());   // refuses cancelled appointments first
        return queueService.checkIn(r.patientId(), r.appointmentId(), r.priority());
    }

    @PostMapping("/queue/next")
    public ResponseEntity<QueueEntry> next() {
        QueueEntry next = queueService.callNext();
        if (next == null) return ResponseEntity.noContent().build();
        appointmentService.markStatus(next.getAppointmentId(), Appointment.Status.IN_PROGRESS);
        return ResponseEntity.ok(next);
    }

    record UpdateRequest(LocalDateTime appointmentTime, String reason) {}

    @PutMapping("/appointments/{id}")
    public Appointment update(@PathVariable int id, @RequestBody UpdateRequest r) {
        return appointmentService.reschedule(id, r.appointmentTime(), r.reason());
    }

    @DeleteMapping("/appointments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        appointmentService.delete(id);
    }
}