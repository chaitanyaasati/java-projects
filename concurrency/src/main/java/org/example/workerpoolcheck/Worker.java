package org.example.workerpoolcheck;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

public class Worker implements Runnable {

    private final BlockingQueue<QueueRecord> queue = new ArrayBlockingQueue<>(10);

    public synchronized CompletableFuture<String> submit(Task task) {
        CompletableFuture<String> future = new CompletableFuture<>();
        queue.add(QueueRecord.createTask(task, future));
        return future;
    }

    public synchronized void shutdown(){
        queue.add(QueueRecord.stopTask());
    }

    public void run() {
        while(true) {
            try {
                QueueRecord record = queue.take();
                if(record.stop){
                    break;
                }
                try{
                    String result = record.task.execute();
                    record.future.complete(result);
                } catch (InterruptedException e) {
                    record.future().completeExceptionally(e);
                    throw e;
                } catch (RuntimeException e) {
                    record.future().completeExceptionally(e);
                    System.err.println("Task failed: " + e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Worker was interrupted");
            }
        }
    }

    private record QueueRecord(Task task, CompletableFuture<String> future, boolean stop){
        public static QueueRecord createTask(Task task, CompletableFuture<String> string){
            return new QueueRecord(task, string, false);
        }
        public static QueueRecord stopTask(){
            return new QueueRecord(null, null, true);
        }
    }
}
