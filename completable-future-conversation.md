# CompletableFuture Conversation

## What is `CompletableFuture`?

**User:** What is completable future?

**Assistant:** `CompletableFuture` is a Java class (`java.util.concurrent.CompletableFuture`) used to run tasks asynchronously and combine their results without manually managing threads or blocking immediately.

```java
CompletableFuture<String> future =
    CompletableFuture.supplyAsync(() -> "Hello");

future.thenApply(message -> message + " World")
      .thenAccept(System.out::println);
```

It represents a value that may become available later and provides operations such as `thenApply`, `thenCompose`, `thenCombine`, `exceptionally`, `join`, and `allOf`.

## Role of the manually created future

**User:** In `concurrency/src/main/java/org/example/personalpool/Main.java`, we are just initializing `CompletableFuture`. What is its role?

**Assistant:** In `Worker.java`, this statement:

```java
CompletableFuture<String> result = new CompletableFuture<>();
```

creates an incomplete placeholder for a task's future result. It does not start a thread or execute the task.

Its lifecycle is:

1. `submit()` creates the future.
2. The future is stored beside the task in the queue.
3. The same future is returned to `Main`.
4. The worker thread executes the task.
5. The worker supplies the answer with `entry.result().complete(result)`.
6. Completing it triggers registered callbacks such as `thenAccept`.

The worker performs the asynchronous work; the future represents and communicates the eventual outcome.

## Which thread executes `thenAccept`?

**User:** The callback tells you what to do after the task finishes, but which thread executes it? How do the threads communicate?

**Assistant:** In this code, a non-async callback registered with `thenAccept()` normally runs in the worker thread named `api-worker`, because that thread calls `complete()`.

```text
main thread                        api-worker thread
-----------                        -----------------
submit(task)
receive incomplete future
register thenAccept()
                                   execute task
                                   future.complete(result)
                                   run thenAccept callback
```

The shared, thread-safe `CompletableFuture` communicates the result. If the future is already complete when `thenAccept()` is registered, the registering thread may execute the callback immediately.

`thenAcceptAsync()` instead schedules the callback on an executor, using `ForkJoinPool.commonPool()` by default unless a specific executor is supplied.

## Does `complete()` trigger `thenAccept()`?

**User:** So when I call `complete`, is `thenAccept` triggered?

**Assistant:** Yes. Calling `complete(value)` stores the result, marks the future completed, and triggers eligible dependent callbacks.

```java
CompletableFuture<String> future = new CompletableFuture<>();

future.thenAccept(value ->
    System.out.println("Received: " + value));

future.complete("Hello");
```

Only the first completion succeeds. Subsequent calls return `false`. If the future is completed exceptionally, a normal `thenAccept` callback is skipped.

## How `complete(T value)` works internally

**User:** Tell me about this method:

```java
public boolean complete(T value) {
    boolean triggered = completeValue(value);
    postComplete();
    return triggered;
}
```

**Assistant:** `completeValue(value)` atomically attempts to change the future from incomplete to normally completed and store the value. Only one competing thread can successfully complete it.

`postComplete()` processes dependent stages and waiting threads. Non-async callbacks may run in the thread calling `complete()`; async callbacks are submitted to their executor.

The returned boolean indicates whether this invocation successfully completed the future:

```java
future.complete("First");  // true
future.complete("Second"); // false
future.join();              // "First"
```

## How `firstResult.join()` works

**User:** How would `firstResult.join()` act and work?

**Assistant:** `join()` returns the future's result. If the result is not ready, it blocks the calling thread until the future completes.

```text
main thread                         api-worker thread
-----------                         -----------------
submit first task
receive incomplete future
call firstResult.join()
main thread waits  ----------->     execute task
                                    complete(result)
main thread resumes <----------
join() returns result
```

If the future is already complete, `join()` returns immediately. If it completed exceptionally, `join()` throws an unchecked `CompletionException` wrapping the original failure.

`firstResult.join()` waits for one future and returns its value, whereas `thread.join()` waits for the entire worker thread to terminate and returns no value.

## Who wakes the waiting main thread?

**User:** If the main thread is waiting, how is it brought back, and who does it?

**Assistant:** When `join()` finds the future incomplete, its waiting machinery can park the main thread. When the worker calls `complete(result)`, completion processing stores the result, marks the future complete, and unparks threads waiting for it.

```text
Main thread                          Worker thread
-----------                          -------------
firstResult.join()
register as waiter
park
                                     complete(result)
                                     process waiters
                                     unpark(main thread)
main becomes runnable
JVM/OS schedules main
join() reads and returns result
```

The JVM and operating-system scheduler decide exactly when the runnable main thread receives CPU time.

## What is `LockSupport`?

**User:** What is `LockSupport`?

**Assistant:** `java.util.concurrent.locks.LockSupport` is a low-level concurrency utility that can pause the current thread and give a specific thread permission to continue:

```java
LockSupport.park();          // May pause the current thread
LockSupport.unpark(thread);  // Give a thread a permit to continue
```

Each thread conceptually has at most one permit. If a permit is already available, `park()` consumes it and returns immediately. This makes it safe for `unpark()` to occur before `park()`.

Because `park()` can return spuriously, it should be used inside a condition-checking loop. Application code should generally prefer higher-level concurrency utilities such as futures, locks, and blocking queues.

## Can multiple threads wait on one future?

**User:** Can multiple threads have the same future and all call `join()` while waiting for its result?

**Assistant:** Yes. Multiple threads may safely share one `CompletableFuture` and call `join()` on it.

```java
CompletableFuture<String> future = new CompletableFuture<>();

Thread t1 = new Thread(() -> System.out.println(future.join()));
Thread t2 = new Thread(() -> System.out.println(future.join()));
Thread t3 = new Thread(() -> System.out.println(future.join()));

t1.start();
t2.start();
t3.start();

future.complete("Done");
```

Once the future is completed, all waiting threads become eligible to continue and receive the same result. Their execution order is not guaranteed. Threads calling `join()` after completion receive the result immediately.

