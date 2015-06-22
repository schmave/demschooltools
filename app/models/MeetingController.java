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
        // For some reason this causes an infinite loop if you do:
        //
        //  Meeting m = Meeting.find.byId(meeting_id);
        //  m.cases.size();
        //java.util.Collections.sort(m.cases);
	}
}
