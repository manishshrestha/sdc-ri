package org.ieee11073.sdc.dpws.soap.interception;

import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry to hold and provide a set of interceptor objects.
 */
public class InterceptorRegistry {
    private final Map<Direction, Map<String, List<InterceptorInfo>>> interceptorChains;

    @Inject
    InterceptorRegistry() {
        interceptorChains = new HashMap<>();
        Arrays.stream(Direction.values()).forEach(direction ->
                interceptorChains.put(direction, new HashMap<>()));
    }

    /**
     * Take object and search for interceptor methods.
     *
     * Interceptor methods are stored for fast access into a registry. They are retrievable using
     *
     * - {@link #getDefaultInterceptors()}
     * - {@link #getDefaultInterceptors(Direction)}
     * - {@link #getInterceptors(String)}
     * - {@link #getInterceptors(Direction, String)}
     *
     * To annotate interceptor methods, use {@link MessageInterceptor}.
     *
     * @param interceptor The object where to search for interceptor methods.
     */
    public void addInterceptor(Object interceptor) {
        List<Method> actionCallbackMethods = getCallbackMethods(interceptor);
        actionCallbackMethods.forEach(method -> {
            MessageInterceptor annotation = method.getAnnotation(MessageInterceptor.class);
            InterceptorInfo interceptorInfo = new InterceptorInfo(interceptor, method, annotation.sequenceNumber());
            Direction direction = annotation.direction();
            String action = annotation.value();

            if (direction != Direction.ANY) {
                List<InterceptorInfo> interceptorInfoList = getInterceptorInfoList(Direction.ANY, action);
                interceptorInfoList.add(interceptorInfo);
                Collections.sort(interceptorInfoList);
            }

            List<InterceptorInfo> interceptorInfoList = getInterceptorInfoList(direction, action);
            interceptorInfoList.add(interceptorInfo);
            Collections.sort(interceptorInfoList);
        });
    }

    /**
     * Retrieve all interceptors of any direction and action.
     */
    public List<InterceptorInfo> getDefaultInterceptors() {
        return getInterceptors("");
    }

    /**
     * Retrieve all interceptors with given direction.
     */
    public List<InterceptorInfo> getDefaultInterceptors(Direction direction) {
        return getInterceptors(direction, "");
    }

    /**
     * Retrieve all interceptors with given direction and action.
     */
    public List<InterceptorInfo> getInterceptors(Direction direction, String action) {
        return getInterceptorInfoList(direction, action);
    }

    /**
     * Retrieve all interceptors of any direction with given action.
     */
    public List<InterceptorInfo> getInterceptors(String action) {
        return getInterceptorInfoList(Direction.ANY, action);
    }

    private List<Method> getCallbackMethods(Object obj) {
        final Method[] declaredMethods = obj.getClass().getDeclaredMethods();
        return Arrays.asList(declaredMethods)
                .parallelStream()
                .filter(m -> Arrays.asList(m.getAnnotations())
                        .parallelStream()
                        .filter(a -> a.annotationType().equals(MessageInterceptor.class))
                        .findFirst().isPresent())
                .filter(m -> m.getParameterCount() == 1 &&
                        InterceptorCallbackType.class.isAssignableFrom(m.getParameterTypes()[0]))
                .filter(m -> m.getReturnType().equals(InterceptorResult.class) ||
                        m.getReturnType().equals(Void.TYPE))
                .map(method -> {
                    method.setAccessible(true);
                    return method;
                })
                .collect(Collectors.toList());
    }

    private List<InterceptorInfo> getInterceptorInfoList(Direction direction, String action) {
        Map<String, List<InterceptorInfo>> actionMap = interceptorChains.get(direction);
        return actionMap.computeIfAbsent(action, k -> new ArrayList<>());
    }
}
