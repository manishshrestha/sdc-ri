package org.somda.sdc.common.util;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

public class ExecutorWrapperUtil {

    /**
     * Binds a {@linkplain ScheduledExecutorService} wrapped in a {@linkplain Callable} to an annotation.
     * <p>
     * The callable will be wrapped inside a {@linkplain ExecutorWrapperService} before being bound to enable
     * {@linkplain com.google.common.util.concurrent.Service} functionality for the executor.
     *
     * @param module     configuration module to bind executor to.
     * @param executor   wrapped inside a {@linkplain Callable}.
     * @param annotation to be bound to.
     */
    public static void bindScheduledExecutor(AbstractModule module, Callable<ScheduledExecutorService> executor, Class<? extends Annotation> annotation) {
        var executorWrapper = new ExecutorWrapperService<>(executor, annotation.getSimpleName());
        var tl = new TypeLiteral<ExecutorWrapperService<ScheduledExecutorService>>() {
        };

        try {
            Method bindMethod = getBindMethod(module);
            Object invoke = bindMethod.invoke(module, tl);

            AnnotatedBindingBuilder<ExecutorWrapperService<ScheduledExecutorService>> invokeResult = (AnnotatedBindingBuilder<ExecutorWrapperService<ScheduledExecutorService>>) invoke;

            invokeResult.annotatedWith(annotation).toInstance(executorWrapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Binds a {@linkplain ListeningExecutorService} wrapped in a {@linkplain Callable} to an annotation.
     * <p>
     * The callable will be wrapped inside a {@linkplain ExecutorWrapperService} before being bound to enable
     * {@linkplain com.google.common.util.concurrent.Service} functionality for the executor.
     *
     * @param module     configuration module to bind executor to.
     * @param executor   wrapped inside a {@linkplain Callable}.
     * @param annotation to be bound to.
     */
    public static <T extends AbstractModule> void bindListeningExecutor(T module, Callable<ListeningExecutorService> executor, Class<? extends Annotation> annotation) {
        var executorWrapper = new ExecutorWrapperService<>(executor, annotation.getSimpleName());
        var tl = new TypeLiteral<ExecutorWrapperService<ListeningExecutorService>>() {
        };

        try {
            Method bindMethod = getBindMethod(module);
            Object invoke = bindMethod.invoke(module, tl);

            var invokeResult = (AnnotatedBindingBuilder<ExecutorWrapperService<ListeningExecutorService>>) invoke;

            invokeResult.annotatedWith(annotation).toInstance(executorWrapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Method getBindMethod(AbstractModule module) throws Exception {
        Class<? extends AbstractModule> clzz = module.getClass();
        Class<AbstractModule> target = AbstractModule.class;
        while (clzz != null && !(clzz.isAssignableFrom(target))) {
            clzz = (Class<? extends AbstractModule>) clzz.getSuperclass();
        }

        Method bindMethod = clzz.getDeclaredMethod("bind", TypeLiteral.class);
        bindMethod.setAccessible(true);
        return bindMethod;
    }
}
