# CompletableFuture Conversation

## User

Refer `concurrency/src/main/java/org/example/compfuture`, what are different ways to wait for a `CompletableFuture` to complete in the main thread?

## Assistant

In `Main.java`, the main thread currently waits using:

```java
response.get();
```

The main ways to wait are:

### 1. `get()` — checked exceptions

```java
String result = response.get();
```

- Blocks until completion.
- Throws `InterruptedException` and `ExecutionException`.
- Useful when interruption must be handled explicitly.

### 2. `join()` — simpler, unchecked exception

```java
String result = response.join();
```

- Blocks until completion.
- Wraps failure in `CompletionException`.
- Usually the cleanest choice in application code.

### 3. Timed `get()` — bounded waiting

```java
String result = response.get(5, TimeUnit.SECONDS);
```

- Stops waiting after the specified timeout.
- Can throw `TimeoutException`.
- Useful when indefinite blocking is unacceptable.

### 4. `orTimeout()` followed by `join()` or `get()`

```java
String result = response
        .orTimeout(5, TimeUnit.SECONDS)
        .join();
```

The future completes exceptionally if it has not finished within five seconds.

### 5. Wait for multiple futures with `allOf()`

```java
CompletableFuture<Void> all = CompletableFuture.allOf(first, second, third);
all.join();
```

This waits until every supplied future finishes. Retrieve individual results afterward:

```java
String firstResult = first.join();
String secondResult = second.join();
```

### 6. Wait for any future with `anyOf()`

```java
Object firstResult = CompletableFuture.anyOf(first, second, third).join();
```

This unblocks when the first future completes.

A completion callback such as `whenComplete()` does not make the main thread wait. That is why the code still needs `get()` or `join()` at the end. Avoid using `Thread.sleep()` to keep `main` alive because it guesses how long the operation needs and provides no proper result or exception handling.

## User

Does its completion mean all chained operations are also complete?

## Assistant

It depends on which `CompletableFuture` you wait for.

```java
response.whenComplete(...);
response.get();
```

`response.get()` waits only for the original `supplyAsync()` stage. `whenComplete()` creates and returns a new future, but it is discarded here. Therefore, `response.get()` does not formally guarantee that the callback has finished.

Wait on the returned chained future instead:

```java
CompletableFuture<String> completed = response.whenComplete((result, ex) -> {
    if (ex != null) {
        ex.printStackTrace();
    } else {
        System.out.println("Task response: " + result);
    }
});

completed.join();
```

Waiting for the last returned future guarantees that all earlier stages in that chain have completed:

```java
CompletableFuture<Void> chain = CompletableFuture
        .supplyAsync(() -> "Hello")
        .thenApply(String::toUpperCase)
        .thenAccept(System.out::println);

chain.join();
```

If a callback starts another asynchronous future but does not compose or return it, the outer chain will not wait for that nested work:

```java
// Detached work
response.thenAccept(value -> {
    CompletableFuture.runAsync(() -> doMoreWork());
}).join();
```

Use `thenCompose()` to include it in the chain:

```java
response.thenCompose(value ->
    CompletableFuture.runAsync(() -> doMoreWork())
).join();
```

Completion means the stage being waited on and all of its composed predecessors are complete—not every operation that happens to have been launched from them.

## User

Create a Java class under `concurrency/src/main/java/org/example/compfuture` which calls a third-party public API that accepts some parameters and returns a result.

## Assistant

Created `ThirdPartyApiExample.java`. It:

- Calls the public Open-Meteo weather API.
- Accepts latitude and longitude as command-line parameters.
- Uses Java 17's asynchronous `HttpClient.sendAsync()`.
- Validates coordinates and HTTP status codes.
- Waits on the final chained `CompletableFuture` using `join()`.
- Defaults to Hyderabad's coordinates when arguments are omitted.

Example invocation:

```bash
mvn -pl concurrency exec:java \
  -Dexec.mainClass=org.example.compfuture.ThirdPartyApiExample \
  -Dexec.args="12.9716 77.5946"
```

The project compiles successfully with Maven.

## User

Is it a good practice to make all methods return a `CompletableFuture` and let the client decide whether it wants synchronous or asynchronous activity?

## Assistant

It can be a good API design, but not as a blanket rule for every method.

Returning `CompletableFuture<T>` is appropriate for inherently asynchronous work such as HTTP or database calls, independent parallel operations, message publishing, and long-running I/O. The caller can choose asynchronous composition:

```java
weatherClient.fetchWeather(lat, lon)
        .thenAccept(System.out::println);
```

Or synchronous waiting:

```java
String weather = weatherClient.fetchWeather(lat, lon).join();
```

Avoid wrapping ordinary fast synchronous methods merely to make everything return futures. Returning a future also does not automatically make blocking work asynchronous:

```java
// Misleading: the caller is blocked before receiving the future.
CompletableFuture<String> fetchWeather() {
    String result = blockingHttpCall();
    return CompletableFuture.completedFuture(result);
}
```

If the underlying operation is blocking, explicitly choose an executor:

```java
CompletableFuture<String> fetchWeather() {
    return CompletableFuture.supplyAsync(
            this::blockingHttpCall,
            ioExecutor
    );
}
```

A useful API design is to expose an asynchronous method and optionally provide a synchronous convenience wrapper:

```java
public CompletableFuture<Weather> fetchWeatherAsync(...) {
    // Genuinely asynchronous implementation
}

public Weather fetchWeather(...) {
    return fetchWeatherAsync(...).join();
}
```

Avoid blocking with `join()` in the middle of an asynchronous chain. Compose instead:

```java
// Avoid
firstCall().thenApply(value -> secondCall(value).join());

// Prefer
firstCall().thenCompose(this::secondCall);
```

Return `CompletableFuture` from genuinely asynchronous operations, keep simple transformations synchronous, and let the outermost caller decide where—or whether—to block.

## User

How can I convert a synchronous method into an asynchronous one?

## Assistant

Wrap a synchronous value-returning call with `CompletableFuture.supplyAsync()`:

```java
private final ExecutorService ioExecutor =
        Executors.newFixedThreadPool(10);

public CompletableFuture<String> fetchWeatherAsync(
        double latitude,
        double longitude) {
    return CompletableFuture.supplyAsync(
            () -> fetchWeather(latitude, longitude),
            ioExecutor
    );
}
```

For a synchronous method returning `void`, use `runAsync()`:

```java
public CompletableFuture<Void> sendNotificationAsync(String message) {
    return CompletableFuture.runAsync(
            () -> sendNotification(message),
            ioExecutor
    );
}
```

Wrapping a method moves its blocking work to another thread; it does not transform the underlying operation into true non-blocking I/O. Use a dedicated executor for blocking HTTP, database, or file calls, and close an owned executor during application shutdown.

General rule:

```java
// Returns a value
CompletableFuture.supplyAsync(() -> synchronousMethod(), executor);

// Returns void
CompletableFuture.runAsync(() -> synchronousMethod(), executor);
```

## User

Why do we need a separate executor? Can it use `ForkJoinPool`?

## Assistant

Yes, it can use `ForkJoinPool.commonPool()`:

```java
CompletableFuture<String> future =
        CompletableFuture.supplyAsync(this::syncMethod);
```

When no executor is supplied, `CompletableFuture` normally uses the common pool.

For short CPU-bound work, the common pool is usually suitable. If the method blocks on HTTP, a database, disk, locks, or long waits, a separate executor is safer. The common pool has a limited number of workers, generally based on available CPU cores. Blocking those workers can delay unrelated `CompletableFuture`, parallel stream, application, and library work that shares the pool.

A dedicated executor provides workload isolation and lets the application control concurrency:

```java
ExecutorService ioExecutor = new ThreadPoolExecutor(
        10,
        20,
        60, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(100),
        new ThreadPoolExecutor.CallerRunsPolicy()
);
```

Rule of thumb:

- Short, CPU-bound work: the common `ForkJoinPool` is reasonable.
- Blocking I/O: use a dedicated, bounded executor.
- An already asynchronous API such as `HttpClient.sendAsync()`: use its returned future directly rather than wrapping it in `supplyAsync()`.
- A small demo or command-line program: the common pool may be acceptable.
- A production server or library: avoid silently consuming the globally shared common pool with blocking work.

An executor does not make an operation non-blocking. It means the operation blocks a worker thread instead of the caller's thread.
