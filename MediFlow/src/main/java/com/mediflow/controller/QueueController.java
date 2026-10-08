package com.mediflow.controller;

import com.mediflow.model.QueueEntry;
import com.mediflow.service.QueueService;

import java.util.List;

public class QueueController {
    private final QueueService queueService;

    public QueueController(QueueService queueService) { this.queueService = queueService; }

    public QueueEntry checkIn(int patientId, int appointmentId, QueueEntry.Priority priority) {
        return queueService.checkIn(patientId, appointmentId, priority);
    }
    public List<QueueEntry> currentQueue() { return queueService.waitingOrderedByPriority(); }
    public QueueEntry callNextPatient() { return queueService.callNext(); }
    public void markDone(int queueId) { queueService.markDone(queueId); }
}
