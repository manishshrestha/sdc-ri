package org.ieee11073.sdc.biceps.common.helper;

import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.common.MdibEntity;
import org.ieee11073.sdc.biceps.common.factory.MdibEntityFactory;
import org.ieee11073.sdc.common.helper.ObjectUtil;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MdibStorageUtil {
    private final ObjectUtil objectUtil;
    private final MdibEntityFactory entityFactory;

    @Inject
    MdibStorageUtil(ObjectUtil objectUtil,
                    MdibEntityFactory entityFactory) {

        this.objectUtil = objectUtil;
        this.entityFactory = entityFactory;
    }

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

    public <T> List<T> exposeListOfImmutables(Collection<T> collection) {
        return collection.stream().map(instance -> objectUtil.immutableFacade(instance)).collect(Collectors.toList());
    }

    public <T, V> List<T> exposeListOfType(Collection<V> collection, Class<T> clazz) {
        return collection.stream()
                .filter(instance -> instance.getClass().equals(clazz))
                .map(instance -> (T) instance)
                .collect(Collectors.toList());
    }

    public List<MdibEntity> exposeEntityList(Map<String, MdibEntity> entities, Collection<String> collection) {
        return collection.stream()
                .filter(handle -> entities.get(handle) != null)
                .map(handle -> entities.get(handle))
                .collect(Collectors.toList());
    }

    public Optional<MdibEntity> exposeEntity(@Nullable MdibEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(entityFactory.createShallowCopy(entity));
    }
}
