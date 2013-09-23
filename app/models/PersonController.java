package models;

import java.util.Set;

import com.avaje.ebean.event.BeanPersistAdapter;
import com.avaje.ebean.event.BeanPersistRequest;

public class PersonController extends BeanPersistAdapter {

	@Override
	public boolean isRegisterFor(Class<?> cls) {
		return cls.equals(Person.class);
	}

	@Override
	public void postLoad(Object bean, Set<String> includedProperties) {
        Person p = (Person)bean;
        p.loadTags();
	}
}
