package test.org.ieee11073.common;

import java.util.Optional;
import java.util.concurrent.*;

public class FutureCondition<T> implements Future<T>, Callable<T> {
    private T result;
    private FutureTask<T> futureTask;

    public FutureCondition() {
        this.result = null;
        this.futureTask = new FutureTask<>(this);
    }

    public void setResult(T result) {
        this.result = result;
        this.futureTask.run();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return futureTask.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return futureTask.isCancelled();
    }

    @Override
    public boolean isDone() {
        return futureTask.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return futureTask.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return futureTask.get(timeout, unit);
    }

    @Override
    public T call() throws Exception {
        if (result == null) {
            throw new Exception("No result available");
        }
        return result;
    }
}
