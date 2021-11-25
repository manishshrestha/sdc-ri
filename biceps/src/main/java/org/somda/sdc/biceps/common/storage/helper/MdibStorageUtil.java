package org.somda.sdc.biceps.common.storage.helper;

import com.google.inject.Inject;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.factory.MdibEntityFactory;
import org.somda.sdc.common.util.ObjectUtil;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Helper class to expose information hosted by {@linkplain org.somda.sdc.biceps.common.storage.MdibStorage}.
 * <p>
 * {@linkplain MdibStorageUtil} is used for but not limited to type-dependent data exposition.
 */
public class MdibStorageUtil {
    private final ObjectUtil objectUtil;
    private final MdibEntityFactory entityFactory;

    @Inject
    MdibStorageUtil(ObjectUtil objectUtil,
                    MdibEntityFactory entityFactory) {

        this.objectUtil = objectUtil;
        this.entityFactory = entityFactory;
    }

    /**
     * Accepts an object of a certain instance and cast it to a given type.
     *
     * @param instance the instance to cast.
     * @param clazz    the class that the instance is supposed to be cast to.
     * @param <T>      the resulting type.
     * @return a cast version of {@code instance} or {@linkplain Optional#empty()} if instance is null or not assignable
     * from the given type.
     */
    public <T> Optional<T> exposeInstance(@Nullable Object instance,
                                          Class<T> clazz) {
        if (instance == null) {
            return Optional.empty();
        }

        if (clazz.isAssignableFrom(instance.getClass())) {
            return Optional.of((T) instance);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Accepts a collection of objects and cast it to a list of the given type.
     *
     * @param collection the collection with objects to cast.
     * @param clazz      the class that the collection is supposed to carry.
     * @param <T>        the resulting type.
     * @param <V>        the collection type.
     * @return a cast version of {@code collection} as list that only includes assignable objects.
     */
    public <T, V> List<T> exposeListOfType(Collection<V> collection, Class<T> clazz) {
        return collection.stream()
                .filter(instance -> instance.getClass().equals(clazz))
                .map(instance -> (T) instance)
                .collect(Collectors.toList());
    }

    /**
     * Takes a map of entities and a collection of handles and converts it to a list of entities.
     *
     * @param entities   the entities to access.
     * @param collection an ordered collection of handles.
     * @return a list of entities sorted by the order of {@code collection}. Entities that cannot be found in
     * {@code entities} are omitted from the result.
     */
    public List<MdibEntity> exposeEntityList(Map<String, MdibEntity> entities, Collection<String> collection) {
        return collection.stream()
                .filter(handle -> entities.get(handle) != null)
                .map(entities::get)
                .collect(Collectors.toList());
    }
}
