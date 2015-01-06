package models;

import java.util.Set;

import com.avaje.ebean.event.BeanPersistAdapter;
import com.avaje.ebean.event.BeanPersistRequest;

public class MeetingController extends BeanPersistAdapter {

	@Override
	public boolean isRegisterFor(Class<?> cls) {
		return cls.equals(Meeting.class);
	}

	@Override
	public void postLoad(Object bean, Set<String> includedProperties) {
        Meeting m = (Meeting)bean;
        // workaround for OrderBy annotation being ignored.
        java.util.Collections.sort(m.cases);
	}
}
