package org.example.personalpool;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Worker implements Runnable {

    private final BlockingQueue<QueueEntry> taskQueue = new LinkedBlockingQueue<>();
    private boolean acceptingTasks = true;

    public synchronized void submit(Task task) {
        if (!acceptingTasks) {
            throw new IllegalStateException("Worker has been shut down");
        }
        taskQueue.add(QueueEntry.task(Objects.requireNonNull(task, "task")));
    }

    public synchronized void shutdown() {
        if (acceptingTasks) {
            acceptingTasks = false;
            taskQueue.add(QueueEntry.stopEntry());
        }
    }

    @Override
    public void run() {
        System.out.println("Worker is running");

        try {
            while (true) {
                // Wait here without consuming CPU until a producer submits a task.
                QueueEntry entry = taskQueue.take();
                if (entry.stop()) {
                    break;
                }

                try {
                    String result = entry.task().execute();
                    System.out.println("API response: " + result);
                } catch (RuntimeException e) {
                    // One failed task should not terminate the worker.
                    System.err.println("Task failed: " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Worker was interrupted");
        }

        System.out.println("Worker completed all tasks");
    }

    private record QueueEntry(Task task, boolean stop) {
        private static QueueEntry task(Task task) {
            return new QueueEntry(task, false);
        }

        private static QueueEntry stopEntry() {
            return new QueueEntry(null, true);
        }
    }
}
