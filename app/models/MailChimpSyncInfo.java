package models;

import java.util.*;

public class MailChimpSyncInfo {

    public Map<String, ListRecord> list_changes =
        new HashMap<String, ListRecord>();

    public class ListRecord {
        public List<Person> adds = new ArrayList<Person>();
        public List<Person> removes = new ArrayList<Person>();
        public List<Person> updates = new ArrayList<Person>();
        public List<String> errors = new ArrayList<String>();
    }

    public boolean isEmpty() {
        for (ListRecord r: list_changes.values()) {
            if (r.adds.size() > 0 ||
                r.removes.size() > 0 ||
                r.updates.size() > 0 ||
                r.errors.size() > 0) {
                return false;
            }
        }

        return true;
    }

    ListRecord getOrCreate(String list_id) {
        if (list_changes.containsKey(list_id)) {
            return list_changes.get(list_id);
        } else {
            ListRecord r = new ListRecord();
            list_changes.put(list_id, r);
            return r;
        }
    }

    public void add(String list_id, Person p) {
        getOrCreate(list_id).adds.add(p);
    }

    public void remove(String list_id, Person p) {
        getOrCreate(list_id).removes.add(p);
    }

    public void update(String list_id, Person p) {
        getOrCreate(list_id).updates.add(p);
    }

    public void error(String list_id, String msg) {
        getOrCreate(list_id).errors.add(msg);
    }
}

