package org.somda.sdc.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Date-time object that supports local and zoned time depending on construction parameters.
 * <p>
 * Required as both local and zoned representation are supported by XML Schema DateTime.
 */
public class AnyDateTime {
    private final static ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    private LocalDateTime local;
    private ZonedDateTime zoned;

    /**
     * Creates an instance with local date and time.
     *
     * @param local the local time to set.
     * @return a new instance.
     */
    public static AnyDateTime create(LocalDateTime local) {
        return new AnyDateTime(local);
    }

    /**
     * Creates an instance with zoned date and time.
     *
     * @param zoned the zoned date and time to set.
     * @return a new instance.
     */
    public static AnyDateTime create(ZonedDateTime zoned) {
        return new AnyDateTime(zoned);
    }

    /**
     * Gets the zoned date and time.
     *
     * @return the zoned time or {@link Optional#empty()} if local time was given on construction.
     */
    public Optional<ZonedDateTime> getZoned() {
        return Optional.ofNullable(zoned);
    }

    /**
     * Gets the local date and time.
     *
     * @return the local date and time or {@link Optional#empty()} if zoned date and time was given on construction.
     */
    public Optional<LocalDateTime> getLocal() {
        return Optional.ofNullable(local);
    }

    /**
     * Returns a zoned date and time no matter if a local time was given on construction.
     *
     * @return the zoned date and time given on construction or a zoned date and time based on the local date and time
     * and default-derived timezone.
     */
    public ZonedDateTime forceZoned() {
        return forceZoned(DEFAULT_ZONE);
    }

    /**
     * Returns a zoned date and time no matter if a local date and time was given on construction.
     *
     * @param localOffset the time zone offset to use in case this objects stores local time.
     * @return the zoned time given on construction or a zoned time based on the local time and given timezone.
     */
    public ZonedDateTime forceZoned(ZoneId localOffset) {
        return Objects.requireNonNullElseGet(zoned, () -> ZonedDateTime.of(local, localOffset));
    }

    /**
     * Convenience function to be called if object was created with local date and time.
     *
     * @param consumer the consumer for the local date and time.
     * @return an alternative call, i.e. in case zoned date and time was given.
     */
    public Else<ZonedDateTime> doIfLocal(Consumer<LocalDateTime> consumer) {
        if (local != null) {
            consumer.accept(local);
            return elseConsumer -> {
            };
        }

        return elseConsumer -> elseConsumer.accept(zoned);
    }

    /**
     * Convenience function to be called if object was created with zoned date and time.
     *
     * @param consumer the consumer for the zoned date and time.
     * @return an alternative call, i.e. in case local date and time was given.
     */
    public Else<LocalDateTime> doIfZoned(Consumer<ZonedDateTime> consumer) {
        if (zoned != null) {
            consumer.accept(zoned);
            return elseConsumer -> {
            };
        }

        return elseConsumer -> elseConsumer.accept(local);
    }

    /**
     * An alternative lambda to be executed.
     *
     * @param <T> the date-time type to accept.
     * @see #doIfLocal(Consumer)
     * @see #doIfZoned(Consumer)
     */
    public interface Else<T> {
        void orElse(Consumer<T> consumer);
    }

    private AnyDateTime(LocalDateTime local) {
        this.local = local;
    }

    private AnyDateTime(ZonedDateTime zoned) {
        this.zoned = zoned;
    }
}
