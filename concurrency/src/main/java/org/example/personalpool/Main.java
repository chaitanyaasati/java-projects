package org.example.personalpool;

import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Worker worker = new Worker();
        Thread thread = new Thread(worker, "api-worker");
        thread.start();

        // Tasks can be submitted individually at any time while the worker is running.
        CompletableFuture<String> firstResult = worker.submit(new Task());
        CompletableFuture<String> secondResult = worker.submit(new Task());
        CompletableFuture<String> thirdResult = worker.submit(new Task());

        // These callbacks run when their corresponding task has finished.
        firstResult.thenAccept(result -> System.out.println("Task 1 response: " + result));
        secondResult.thenAccept(result -> System.out.println("Task 2 response: " + result));
        thirdResult.thenAccept(result -> System.out.println("Task 3 response: " + result));

        // Stop accepting work and exit after all submitted tasks are processed.
        worker.shutdown();
        thread.join();

        System.out.println("Processing Completed");
    }
}
