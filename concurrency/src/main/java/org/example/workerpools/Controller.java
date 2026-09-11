package org.example.workerpools;

import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Controller {

    public static BlockingQueue<Info> deque = new DelayQueue<>();
    private static final AtomicInteger taskCounter = new AtomicInteger(0);

    public static void main(String[] args) {
        WorkerPool workerPool = new WorkerPool(deque, 4);

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=== Task Scheduler ===");

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    createTask(scanner);
                    break;
                case "2":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option.. Please enter 1 or 2.");
            }
        }

        System.out.println("Exiting application. Any pending tasks will be cancelled.");
        scanner.close();
        workerPool.shutdown();
    }

    private static void printMenu() {
        System.out.println("\nChoose an option:");
        System.out.println("1. Create a task");
        System.out.println("2. Exit");
        System.out.print("> ");
    }

    private static void createTask(Scanner scanner) {
        double minutes;
        while (true) {
            System.out.print("After how many minutes should the task run? ");
            String input = scanner.nextLine().trim();
            try {
                minutes = Double.parseDouble(input);
                if (minutes < 0) {
                    System.out.println("Please enter a non-negative number.");
                    continue;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }

        System.out.print("What should be printed when the task is done? ");
        String message = scanner.nextLine();

        int taskId = taskCounter.incrementAndGet();
        long delaySeconds = Math.round(minutes * 60);
        long epochSeconds = Instant.now().getEpochSecond();

        System.out.println("Task #" + taskId + " scheduled to run in " + minutes + " minute(s).");

        deque.add(new Info(message, taskId, epochSeconds + delaySeconds));
    }
}
