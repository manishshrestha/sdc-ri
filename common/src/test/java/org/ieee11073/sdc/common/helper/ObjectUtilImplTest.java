package org.ieee11073.sdc.common.helper;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.BasicConfigurator;
import org.ieee11073.sdc.common.helper.JaxbUtil;
import org.ieee11073.sdc.common.helper.JaxbUtilImpl;
import org.ieee11073.sdc.common.helper.ObjectUtil;
import org.ieee11073.sdc.common.helper.ObjectUtilImpl;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ObjectUtilImplTest {

    @Test
    public void copy() {
        BasicConfigurator.configure();
        Injector inj = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ObjectUtil.class).to(ObjectUtilImpl.class).asEagerSingleton();
                    }
                });
        ObjectUtil objectUtil = inj.getInstance(ObjectUtil.class);

        PojoClass obj = new PojoClass("test", 13, Arrays.asList("entry1", "entry2"));
        PojoClass objCopy = objectUtil.deepCopy(obj);

        assertThat(objCopy.getStr(), is("test"));
        assertThat(objCopy.getNum(), is(13));
        assertThat(objCopy.getStrList(), is(Arrays.asList("entry1", "entry2")));

        obj.setStr("test2");

        assertThat(objCopy.getStr(), is("test"));

        List<PojoClass> list = Arrays.asList(new PojoClass("test", 1, new ArrayList<>()),
                new PojoClass("test2", 2, new ArrayList<>()));

        List<PojoClass> listCpy = objectUtil.deepCopy(list);

        assertThat(listCpy.size(), is(2));
        assertThat(listCpy.get(1).getStr(), is("test2"));

        list.get(1).setStr("test3");

        assertThat(listCpy.get(1).getStr(), is("test2"));
    }

    private class PojoClass {
        public PojoClass(String str, int num, List<String> strList) {
            this.str = str;
            this.num = num;
            this.strList = strList;
        }

        private String str;
        private int num;
        private List<String> strList;

        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public int getNum() {
            return num;
        }

        public void setNum(int num) {
            this.num = num;
        }

        public List<String> getStrList() {
            return strList;
        }

        public void setStrList(List<String> strList) {
            this.strList = strList;
        }
    }
}