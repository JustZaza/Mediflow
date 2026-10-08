package com.mediflow.service;

import com.mediflow.dao.QueueDAO;
import com.mediflow.model.QueueEntry;

import java.util.List;
import java.util.PriorityQueue;

/**
 * FR-04 / Section 7: queue management using a java.util.PriorityQueue<QueueEntry>.
 * QueueEntry.compareTo orders EMERGENCY before HIGH before NORMAL, and within the same
 * priority level, earlier queue numbers (i.e. earlier check-in) go first — so the structure
 * gives correct "who's next" behavior with O(log n) insert/poll instead of a plain list scan.
 */
public class QueueService {
    private final QueueDAO queueDAO;

    public QueueService(QueueDAO queueDAO) { this.queueDAO = queueDAO; }

    public QueueEntry checkIn(int patientId, int appointmentId, QueueEntry.Priority priority) {
        int queueNumber = queueDAO.nextQueueNumberForToday();
        QueueEntry entry = new QueueEntry(patientId, appointmentId, priority, queueNumber);
        return queueDAO.insert(entry);
    }

    /** Loads today's waiting patients into a PriorityQueue and returns them in "who's called next" order. */
    public List<QueueEntry> waitingOrderedByPriority() {
        PriorityQueue<QueueEntry> pq = new PriorityQueue<>(queueDAO.findWaitingToday());
        return pq.stream().sorted().toList(); // stream+sorted just renders the pq contents in order for display
    }

    /** Pops the single next patient the doctor should see, and marks them IN_PROGRESS. */
    public QueueEntry callNext() {
        PriorityQueue<QueueEntry> pq = new PriorityQueue<>(queueDAO.findWaitingToday());
        QueueEntry next = pq.poll();
        if (next != null) {
            queueDAO.updateStatus(next.getQueueId(), QueueEntry.Status.IN_PROGRESS);
        }
        return next;
    }

    public void markDone(int queueId) {
        queueDAO.updateStatus(queueId, QueueEntry.Status.DONE);
    }
}
