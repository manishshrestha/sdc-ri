package org.somda.sdc.common.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Date-time object that supports local and offset time depending on construction parameters.
 * <p>
 * Required as both local and offset representation are supported by XML Schema DateTime.
 */
public class AnyDateTime {
    private final static ZoneOffset DEFAULT_OFFSET = OffsetDateTime.now().getOffset();

    private LocalDateTime local;
    private OffsetDateTime offset;

    private AnyDateTime(LocalDateTime local) {
        this.local = local;
    }

    private AnyDateTime(OffsetDateTime offset) {
        this.offset = offset;
    }

    /**
     * Creates an instance with local date and time.
     *
     * @param local the local date and time to set.
     * @return a new instance.
     */
    public static AnyDateTime create(LocalDateTime local) {
        return new AnyDateTime(local);
    }

    /**
     * Creates an instance with offset date and time.
     *
     * @param offset the offset date and time to set.
     * @return a new instance.
     */
    public static AnyDateTime create(OffsetDateTime offset) {
        return new AnyDateTime(offset);
    }

    /**
     * Gets the offset date and time.
     *
     * @return the offset time or {@link Optional#empty()} if local time was given on construction.
     */
    public Optional<OffsetDateTime> getOffset() {
        return Optional.ofNullable(offset);
    }

    /**
     * Gets the local date and time.
     *
     * @return the local date and time or {@link Optional#empty()} if offset date and time was given on construction.
     */
    public Optional<LocalDateTime> getLocal() {
        return Optional.ofNullable(local);
    }

    /**
     * Returns a offset date and time no matter if a local time was given on construction.
     *
     * @return the offset date and time given on construction or a offset date and time based on the local date and time
     * and default-derived timezone.
     */
    public OffsetDateTime forceOffset() {
        return forceOffset(DEFAULT_OFFSET);
    }

    /**
     * Returns a offset date and time no matter if a local date and time was given on construction.
     *
     * @param localOffset the time zone offset to use in case this objects stores local time.
     * @return the offset time given on construction or a offset time based on the local time and given timezone.
     */
    public OffsetDateTime forceOffset(ZoneOffset localOffset) {
        return Objects.requireNonNullElseGet(offset, () -> OffsetDateTime.of(local, localOffset));
    }

    /**
     * Convenience function to be called if object was created with local date and time.
     *
     * @param consumer the consumer for the local date and time.
     * @return an alternative call, i.e. in case offset date and time was given.
     */
    public Else<OffsetDateTime> doIfLocal(Consumer<LocalDateTime> consumer) {
        if (local != null) {
            consumer.accept(local);
            return elseConsumer -> {
            };
        }

        return elseConsumer -> elseConsumer.accept(offset);
    }

    /**
     * Convenience function to be called if object was created with offset date and time.
     *
     * @param consumer the consumer for the offset date and time.
     * @return an alternative call, i.e. in case local date and time was given.
     */
    public Else<LocalDateTime> doIfOffset(Consumer<OffsetDateTime> consumer) {
        if (offset != null) {
            consumer.accept(offset);
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
     * @see #doIfOffset(Consumer)
     */
    public interface Else<T> {
        /**
         * Defines what to do in an else case provoked by {@link #doIfLocal(Consumer)} or {@link #doIfOffset(Consumer)}.
         *
         * @param consumer the code to execute.
         */
        void orElse(Consumer<T> consumer);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AnyDateTime that = (AnyDateTime) o;
        return Objects.equals(local, that.local) &&
                Objects.equals(offset, that.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(local, offset);
    }

    @Override
    public String toString() {
        return ObjectStringifier.stringify(this);
    }
}
