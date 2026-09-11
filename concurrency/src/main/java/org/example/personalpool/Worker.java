package org.example.personalpool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

public class Worker implements Runnable {

    private final BlockingQueue<QueueEntry> taskQueue = new LinkedBlockingQueue<>();
    private boolean acceptingTasks = true;

    public synchronized CompletableFuture<String> submit(Task task) {
        if (!acceptingTasks) {
            throw new IllegalStateException("Worker has been shut down");
        }

        CompletableFuture<String> result = new CompletableFuture<>();
        taskQueue.add(QueueEntry.task(Objects.requireNonNull(task, "task"), result));
        return result;
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
        boolean interrupted = false;

        try {
            while (true) {
                // Wait here without consuming CPU until a producer submits a task.
                QueueEntry entry = taskQueue.take();
                if (entry.stop()) {
                    break;
                }

                try {
                    String result = entry.task().execute();
                    entry.result().complete(result);
                } catch (InterruptedException e) {
                    entry.result().completeExceptionally(e);
                    throw e;
                } catch (RuntimeException e) {
                    // One failed task should not terminate the worker.
                    entry.result().completeExceptionally(e);
                    System.err.println("Task failed: " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            interrupted = true;
            failPendingTasks(e);
            Thread.currentThread().interrupt();
            System.out.println("Worker was interrupted");
        }

        if (!interrupted) {
            System.out.println("Worker completed all tasks");
        }
    }

    private void failPendingTasks(InterruptedException cause) {
        List<QueueEntry> pendingEntries = new ArrayList<>();

        // Synchronizing with submit() prevents a task from being accepted while
        // the worker is transitioning to its stopped state.
        synchronized (this) {
            acceptingTasks = false;
            taskQueue.drainTo(pendingEntries);
        }

        for (QueueEntry entry : pendingEntries) {
            if (!entry.stop()) {
                entry.result().completeExceptionally(
                        new IllegalStateException(
                                "Task was not executed because the worker was interrupted",
                                cause));
            }
        }
    }

    private record QueueEntry(
            Task task,
            CompletableFuture<String> result,
            boolean stop) {

        private static QueueEntry task(Task task, CompletableFuture<String> result) {
            return new QueueEntry(task, result, false);
        }

        private static QueueEntry stopEntry() {
            return new QueueEntry(null, null, true);
        }
    }
}
