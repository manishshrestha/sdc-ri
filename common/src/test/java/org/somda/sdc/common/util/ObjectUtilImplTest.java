package org.somda.sdc.common.util;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import test.org.somda.common.LoggingTestWatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(LoggingTestWatcher.class)
// TODO: Vytas
class ObjectUtilImplTest {

    /*private Injector inj;
    private ObjectUtil objectUtil;

    @BeforeEach
    public void setUp() {
        inj = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ObjectUtil.class).to(ObjectUtilImpl.class).asEagerSingleton();
                    }
                });
        objectUtil = inj.getInstance(ObjectUtil.class);
    }

    @Test
    void deepCopy() {
        StandalonePojoClass obj = new StandalonePojoClass("test", 13, Arrays.asList("entry1", "entry2"));
        StandalonePojoClass objCopy = objectUtil.deepCopy(obj);

        assertThat(objCopy.getStr(), is("test"));
        assertThat(objCopy.getNum(), is(13));
        assertThat(objCopy.getStrList(), is(Arrays.asList("entry1", "entry2")));

        obj.setStr("test2");

        assertThat(objCopy.getStr(), is("test"));

        List<StandalonePojoClass> list = Arrays.asList(new StandalonePojoClass("test", 1, new ArrayList<>()),
                new StandalonePojoClass("test2", 2, new ArrayList<>()));

        List<StandalonePojoClass> listCpy = objectUtil.deepCopy(list);

        assertThat(listCpy.size(), is(2));
        assertThat(listCpy.get(1).getStr(), is("test2"));

        list.get(1).setStr("test3");

        assertThat(listCpy.get(1).getStr(), is("test2"));
    }*/
}