package test.org.somda.common;

import org.somda.sdc.common.util.AutoLock;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class TimedWait<T> {
    private final ReentrantLock reentrantLock;
    private final Condition condition;
    private final Supplier<T> resetSupplier;
    private T data;

    public TimedWait(Supplier<T> resetSupplier) {
        this.resetSupplier = resetSupplier;
        this.reentrantLock = new ReentrantLock();
        this.condition = reentrantLock.newCondition();
        this.data = this.resetSupplier.get();
    }

    public T getData() {
        try (AutoLock ignored = AutoLock.lock(reentrantLock)) {
            return data;
        }
    }

    public void modifyData(Consumer<T> modifierCallback) {
        try (AutoLock ignored = AutoLock.lock(reentrantLock)) {
            modifierCallback.accept(data);
            condition.signalAll();
        }
    }

    public boolean waitForData(Predicate<T> dataCondition, Duration waitTime) {
        try (AutoLock ignored = AutoLock.lock(reentrantLock)) {
            do {
                Instant start = Instant.now();
                try {
                    if (dataCondition.test(data)) {
                        return true;
                    }
                    if (condition.await(waitTime.toMillis(), TimeUnit.MILLISECONDS)) {
                        if (dataCondition.test(data)) {
                            return true;
                        }
                    }
                } catch (InterruptedException e) {
                    return dataCondition.test(data);
                }

                Instant finish = Instant.now();
                waitTime = waitTime.minus(Duration.between(start, finish));
            } while (waitTime.toMillis() > 0);
            return dataCondition.test(data);
        }
    }

    public void reset() {
        try (AutoLock ignored = AutoLock.lock(reentrantLock)) {
            data = resetSupplier.get();
        }
    }
}
