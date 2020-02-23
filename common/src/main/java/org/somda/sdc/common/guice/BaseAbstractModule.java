package org.somda.sdc.common.guice;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import org.somda.sdc.common.util.ExecutorWrapperService;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

public class BaseAbstractModule extends AbstractModule {

    /**
     * Binds a {@linkplain ScheduledExecutorService} wrapped in a {@linkplain Callable} to an annotation.
     * <p>
     * The callable will be wrapped inside a {@linkplain ExecutorWrapperService} before being bound to enable
     * {@linkplain com.google.common.util.concurrent.Service} functionality for the executor.
     *
     * @param executor   wrapped inside a {@linkplain Callable}.
     * @param annotation to be bound to.
     */
    public void bindScheduledExecutor(Callable<ScheduledExecutorService> executor, Class<? extends Annotation> annotation) {
        var executorWrapper = new ExecutorWrapperService<>(executor, annotation.getSimpleName());
        bind(new TypeLiteral<ExecutorWrapperService<ScheduledExecutorService>>() {
        })
                .annotatedWith(annotation)
                .toInstance(executorWrapper);
    }

    /**
     * Binds a {@linkplain ListeningExecutorService} wrapped in a {@linkplain Callable} to an annotation.
     * <p>
     * The callable will be wrapped inside a {@linkplain ExecutorWrapperService} before being bound to enable
     * {@linkplain com.google.common.util.concurrent.Service} functionality for the executor.
     *
     * @param executor   wrapped inside a {@linkplain Callable}.
     * @param annotation to be bound to.
     */
    public void bindListeningExecutor(Callable<ListeningExecutorService> executor, Class<? extends Annotation> annotation) {
        var executorWrapper = new ExecutorWrapperService<>(executor, annotation.getSimpleName());
        bind(new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>() {
        })
                .annotatedWith(annotation)
                .toInstance(executorWrapper);
    }
}
