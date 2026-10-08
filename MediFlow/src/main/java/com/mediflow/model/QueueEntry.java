package com.mediflow.model;

/**
 * One patient's place in today's queue. Named QueueEntry (not "Queue") to avoid
 * clashing with java.util.Queue, which QueueService uses internally via PriorityQueue.
 */
public class QueueEntry implements Comparable<QueueEntry> {
    public enum Priority { EMERGENCY, HIGH, NORMAL }
    public enum Status { WAITING, IN_PROGRESS, DONE, SKIPPED }

    private int queueId;
    private int patientId;
    private int appointmentId;
    private Priority priority = Priority.NORMAL;
    private int queueNumber;
    private Status status = Status.WAITING;
    private String patientName; // convenience field for display, populated by DAO joins

    public QueueEntry() {}

    public QueueEntry(int patientId, int appointmentId, Priority priority, int queueNumber) {
        this.patientId = patientId;
        this.appointmentId = appointmentId;
        this.priority = priority;
        this.queueNumber = queueNumber;
    }

    /** Lower ordinal = higher priority (EMERGENCY < HIGH < NORMAL); ties broken by queue number (FIFO). */
    @Override
    public int compareTo(QueueEntry other) {
        int p = this.priority.ordinal() - other.priority.ordinal();
        if (p != 0) return p;
        return Integer.compare(this.queueNumber, other.queueNumber);
    }

    public int getQueueId() { return queueId; }
    public void setQueueId(int queueId) { this.queueId = queueId; }
    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public int getQueueNumber() { return queueNumber; }
    public void setQueueNumber(int queueNumber) { this.queueNumber = queueNumber; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
}
