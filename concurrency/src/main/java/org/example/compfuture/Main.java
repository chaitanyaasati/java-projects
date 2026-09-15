package org.example.compfuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<String> response = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(3);
                return "Hello World!";
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                return "Error!";
            }
        });
        response.whenComplete((result, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
            }
            System.out.println("Task 3 response: " + result);
        });
        response.get();
    }
}
