package org.ieee11073.sdc.dpws.soap.interception;

import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry to store and access a set of interceptor objects.
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
     * Takes an object and seeks interceptor methods.
     * <p>
     * Interceptor methods are stored in a registry for fast access.
     * They are retrievable by using
     *
     * <ul>
     * <li>{@link #getDefaultInterceptors()}
     * <li>{@link #getDefaultInterceptors(Direction)}
     * <li>{@link #getInterceptors(String)}
     * <li>{@link #getInterceptors(Direction, String)}
     * </ul>
     *
     * @param interceptor the object where to search for interceptor methods.
     */
    public void addInterceptor(Interceptor interceptor) {
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
     * Gets all default interceptors.
     *
     * @return all interceptors of any direction and action.
     */
    public List<InterceptorInfo> getDefaultInterceptors() {
        return getInterceptors("");
    }

    /**
     * Gets default interceptors of a specific direction.
     *
     * @param direction the direction to filter for.
     * @return all interceptors with given direction.
     */
    public List<InterceptorInfo> getDefaultInterceptors(Direction direction) {
        return getInterceptors(direction, "");
    }

    /**
     * Gets default interceptor of a specific direction and action.
     *
     * @param direction the direction to filter for.
     * @param action    the action to filter for.
     * @return all interceptors with given direction and action.
     */
    public List<InterceptorInfo> getInterceptors(Direction direction, String action) {
        return getInterceptorInfoList(direction, action);
    }

    /**
     * Gets interceptors of a specific action.
     *
     * @param action the action to filter for.
     * @return all interceptors of any direction with given action.
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
                        .anyMatch(a -> a.annotationType().equals(MessageInterceptor.class)))
                .filter(m -> m.getParameterCount() == 1 &&
                        InterceptorCallbackType.class.isAssignableFrom(m.getParameterTypes()[0]))
                .filter(m -> m.getReturnType().equals(InterceptorResult.class) ||
                        m.getReturnType().equals(Void.TYPE))
                .peek(method -> method.setAccessible(true))
                .collect(Collectors.toList());
    }

    private List<InterceptorInfo> getInterceptorInfoList(Direction direction, String action) {
        Map<String, List<InterceptorInfo>> actionMap = interceptorChains.get(direction);
        return actionMap.computeIfAbsent(action, k -> new ArrayList<>());
    }
}
