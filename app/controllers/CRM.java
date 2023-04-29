package controllers;

import io.ebean.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.inject.Inject;
import models.*;
import models.Comment;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import play.api.libs.mailer.MailerClient;
import play.data.*;
import play.data.validation.ValidationError;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.*;
import views.html.*;

@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_ALL_ACCESS)
public class CRM extends Controller {

    Application mApplication;
    FormFactory mFormFactory;
    final MessagesApi mMessagesApi;
    static MailerClient sMailer;

    @Inject
    public CRM(final Application app,
               final FormFactory ff,
               final MailerClient mailer,
               MessagesApi messagesApi) {
        mApplication = app;
        mFormFactory = ff;
        sMailer = mailer;
        mMessagesApi = messagesApi;
    }

    public Result recentComments(Http.Request request) {
        return ok(cached_page.render(new CachedPage(CachedPage.RECENT_COMMENTS,
                "All people",
                "crm",
                "recent_comments", Utils.getOrg(request)) {
                @Override
                String render() {
                    List<Comment> recent_comments = Comment.find.query()
                        .fetch("person")
                        .fetch("completed_tasks", FetchConfig.ofQuery())
                        .fetch("completed_tasks.task", FetchConfig.ofQuery())
                        .where().eq("person.organization", Utils.getOrg(request))
                        .orderBy("created DESC").setMaxRows(20).findList();

                    return people_index.render(recent_comments, request, mMessagesApi.preferred(request)).toString();
                }
            }, request, mMessagesApi.preferred(request)));
    }

    public Result person(Integer id, Http.Request request) {
        Person the_person = Person.findById(id, Utils.getOrg(request));

        List<Person> family_members =
            Person.find.query().where().isNotNull("family").eq("family", the_person.getFamily()).
                ne("person_id", the_person.getPersonId()).findList();

        Set<Integer> family_ids = new HashSet<>();
        family_ids.add(the_person.getPersonId());
        for (Person family : family_members) {
            family_ids.add(family.getPersonId());
        }

        List<Comment> all_comments = Comment.find.query().where().in("person_id", family_ids).
            order("created DESC").findList();

        String comment_destination = "<No email set>";
        boolean first = true;
        for (NotificationRule rule :
                NotificationRule.findByType(NotificationRule.TYPE_COMMENT, Utils.getOrg(request))) {
            if (first) {
                comment_destination = rule.getEmail();
                first = false;
            } else {
                comment_destination += ", " + rule.getEmail();
            }
        }


        return ok(family.render(the_person,
            family_members,
            all_comments,
            comment_destination, request, mMessagesApi.preferred(request)));
    }

    public Result jsonPeople(String query, Http.Request request) {
        HashSet<Person> selected_people = new HashSet<>();
        boolean first_time = true;

        for (String term : query.split(" ")) {
            term = term.trim();
            if (term.length() < 2) {
                continue;
            }
            List<Person> people_matched_this_round;

            Expression this_expr =
                    Expr.or(Expr.ilike("lastName", "%" + term + "%"),
                            Expr.ilike("firstName", "%" + term + "%"));
            this_expr = Expr.or(this_expr,
                    Expr.ilike("address", "%" + term + "%"));
            this_expr = Expr.or(this_expr,
                    Expr.ilike("email", "%" + term + "%"));
            this_expr = Expr.or(this_expr,
                Expr.ilike("displayName", "%" + term + "%"));

            people_matched_this_round =
                    Person.find.query().where().add(this_expr)
                            .eq("organization", Utils.getOrg(request))
                            .eq("isFamily", false)
                    .findList();

            List<PhoneNumber> phone_numbers =
                    PhoneNumber.find.query().where().ilike("number", "%" + term + "%")
                    .eq("owner.organization", Utils.getOrg(request))
                    .findList();
            for (PhoneNumber pn : phone_numbers) {
                people_matched_this_round.add(pn.getOwner());
            }

            if (first_time) {
                selected_people.addAll(people_matched_this_round);
            } else {
                selected_people.retainAll(people_matched_this_round);
            }

            first_time = false;
        }

        List<Person> sorted_people = new ArrayList<>(selected_people);
        sorted_people.sort(Person.SORT_FIRST_NAME);
        if (sorted_people.size() > 30) {
            sorted_people = sorted_people.subList(0, 29);
        }

        List<Map<String, String> > result = new ArrayList<>();
        for (Person p : sorted_people) {
            HashMap<String, String> values = new HashMap<>();
            String label = p.getFirstName();
            if (p.getLastName() != null) {
                label = label + " " + p.getLastName();
            }
            if (p.getDisplayName() != null && !p.getDisplayName().equals("")) {
                label += " (\"" + p.getDisplayName() + "\")";
            }
            values.put("label", label);
            values.put("id", "" + p.getPersonId());
            result.add(values);
        }

        return ok(Json.stringify(Json.toJson(result)));
    }

    // personId should be -1 if the client doesn't care about a particular
    // person, but just wants a list of available tags. In that case,
    // the "create new tag" functionality is also disabled.
    public Result jsonTags(String term, Integer personId, Http.Request request) {
        List<Tag> selected_tags =
            Tag.find.query().where()
                .eq("organization", Utils.getOrg(request))
                .eq("showInMenu", true)
                .ilike("title", "%" + term + "%").findList();

        List<Tag> existing_tags = null;
        if (personId >= 0) {
            Person p = Person.findById(personId, Utils.getOrg(request));
            if (p != null) {
                existing_tags = p.tags;
            }
        }

        List<Map<String, String> > result = new ArrayList<>();
        for (Tag t : selected_tags) {
            if (existing_tags == null || !existing_tags.contains(t)) {
                HashMap<String, String> values = new HashMap<>();
                values.put("label", t.getTitle());
                values.put("id", "" + t.getId());
                result.add(values);
            }
        }

        boolean tag_already_exists = false;
        for (Tag t : selected_tags) {
            if (t.getTitle().equalsIgnoreCase(term)) {
                tag_already_exists = true;
                break;
            }
        }

        if (personId >= 0 && !tag_already_exists) {
            HashMap<String, String> values = new HashMap<>();
            values.put("label", "Create new tag: " + term);
            values.put("id", "-1");
            result.add(values);
        }

        return ok(Json.stringify(Json.toJson(result)));
    }

    public Collection<Person> getTagMembers(Integer tagId, String familyMode, Organization org) {
        List<Person> people = Tag.findById(tagId, org).people;

        Set<Person> selected_people = new HashSet<>(people);

        if (!familyMode.equals("just_tags")) {
            for (Person p : people) {
                if (p.getFamily() != null) {
                    selected_people.addAll(p.getFamily().family_members);
                }
            }
        }

        if (familyMode.equals("family_no_kids")) {
            Set<Person> no_kids = new HashSet<>();
            for (Person p2 : selected_people) {
                if (p2.getDob() == null ||
                        p2.calcAge() > 18) {
                    no_kids.add(p2);
                }
            }
            selected_people = no_kids;
        }

        return selected_people;
    }

    public Result renderTagMembers(Integer tagId, String familyMode, Http.Request request) {
        Organization org = Utils.getOrg(request);
        Tag the_tag = Tag.findById(tagId, org);
        return ok(to_address_fragment.render(the_tag.getTitle(),
                getTagMembers(tagId, familyMode, org), request, mMessagesApi.preferred(request)));
    }

    public Result addTag(Integer tagId, String title, Integer personId, Http.Request request) {
        Organization org = Utils.getOrg(request);
        Person p = Person.findById(personId, org);
        if (p == null) {
            return badRequest();
        }

        Tag the_tag;
        if (tagId == null) {
            the_tag = Tag.create(title, org);
        } else {
            the_tag = Tag.find.ref(tagId);
        }

        addTag(the_tag, p, Application.getCurrentUser(request), request);
        CachedPage.onPeopleChanged(org);
        return ok(tag_fragment.render(the_tag, p, request, mMessagesApi.preferred(request)));
    }

    public void addTag(Tag tag, Person person, User current_user, Http.Request request) {
        if (tag.people.contains(person)) {
            return;
        }

        DB.execute(() -> {
            tag.people.add(person);
            tag.save();

            PersonTagChange.create(
                    tag,
                    person,
                    current_user,
                    true);

            person.tags.add(tag);
            notifyAboutTag(tag, person, true, request);
        });
    }

    public Result removeTag(Integer personId, Integer tag_id, Http.Request request) {
        Tag t = Tag.findById(tag_id, Utils.getOrg(request));
        Person p = Person.findById(personId, Utils.getOrg(request));

        removeTag(t, p, Application.getCurrentUser(request), request);
        CachedPage.onPeopleChanged(Utils.getOrg(request));

        return ok();
    }

    public void removeTag(Tag tag, Person person, User current_user, Http.Request request) {
        if (!tag.people.contains(person)) {
            return;
        }
        DB.execute(() -> {
            if (DB.sqlUpdate("DELETE from person_tag where personId=" + person.getPersonId() +
                    " AND tag_id=" + tag.getId()).execute() == 1) {
                PersonTagChange.create(
                        tag,
                        person,
                        current_user,
                        false);
            }
            notifyAboutTag(tag, person, false, request);
        });
    }

    public void notifyAboutTag(Tag t, Person p, boolean wasAdd, Http.Request request) {
        for (NotificationRule rule : t.notification_rules) {
            play.libs.mailer.Email mail = new play.libs.mailer.Email();
            if (wasAdd) {
                mail.setSubject(p.getInitials() + " added to tag " + t.getTitle());
            } else {
                mail.setSubject(p.getInitials() + " removed from tag " + t.getTitle());
            }
            mail.addTo(rule.getEmail());
            mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
            mail.setBodyHtml(tag_email.render(t,
                    Application.getCurrentUser(request).getName(),
                    p, wasAdd, request, mMessagesApi.preferred(request)).toString());
            sMailer.send(mail);
        }
    }

    public Result allPeople(Http.Request request) {
        return ok(all_people.render(Person.all(Utils.getOrg(request)), request, mMessagesApi.preferred(request)));
    }

    public Result viewAllTags(Http.Request request) {
        List<Tag> tags = Tag.find.query()
            .where().eq("organization", Utils.getOrg(request))
            .orderBy("lower(title)")
            .findList();

        Map<String, Object> scopes = new HashMap<>();
        List<Object> serialized_tags = new ArrayList<>();
        for (Tag tag : tags) {
            serialized_tags.add(tag.serialize());
        }
        scopes.put("tags", serialized_tags);

        return ok(main_with_mustache.render("All Tags",
            "crm",
            "",
            "view_all_tags.html",
            scopes, request, mMessagesApi.preferred(request)));
    }

    public Result editTag(Integer id, Http.Request request) {
        Form<Tag> filled_form = mFormFactory.form(Tag.class).fill(Tag.findById(id, Utils.getOrg(request)));
        return ok(edit_tag.render(filled_form, request, mMessagesApi.preferred(request)));
    }

    public Result saveTag(Http.Request request) {
        Form<Tag> form = mFormFactory.form(Tag.class).bindFromRequest(request);

        if (form.field("id").value().isPresent()) {
            Tag t = Tag.findById(Integer.parseInt(form.field("id").value().get()), Utils.getOrg(request));
            t.updateFromForm(form);

            CachedPage.onPeopleChanged(Utils.getOrg(request));

            return redirect(routes.CRM.viewTag(t.getId()));
        }
        return redirect(routes.CRM.recentComments());
    }

    public Result addPeopleFromTag(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        Tag dest_tag = Tag.findById(Integer.parseInt(values.get("dest_id")[0]), Utils.getOrg(request));
        Tag src_tag = Tag.findById(Integer.parseInt(values.get("tag_id")[0]), Utils.getOrg(request));

        for (Person p : src_tag.people) {
            addTag(dest_tag, p, Application.getCurrentUser(request), request);
        }

        CachedPage.onPeopleChanged(Utils.getOrg(request));
        return redirect(routes.CRM.viewTag(dest_tag.getId()));
    }

    public Result addPeopleToTag(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        Tag dest_tag = Tag.findById(Integer.parseInt(values.get("dest_id")[0]), Utils.getOrg(request));

        for (String person_id_str : values.get("personId")) {
            Person p = Person.findById(Integer.parseInt(person_id_str), Utils.getOrg(request));
            addTag(dest_tag, p, Application.getCurrentUser(request), request);
        }

        CachedPage.onPeopleChanged(Utils.getOrg(request));
        return redirect(routes.CRM.viewTag(dest_tag.getId()));
    }

    public Result removePeopleFromTag(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        Tag tag = Tag.findById(Integer.parseInt(values.get("tag_id")[0]), Utils.getOrg(request));

        String prefix = "person-";
        for (String key_name : values.keySet()) {
            if (key_name.startsWith(prefix) && values.get(key_name)[0].equals("on")) {
                Person p = Person.findById(Integer.parseInt(key_name.substring(prefix.length())), Utils.getOrg(request));
                removeTag(tag, p, Application.getCurrentUser(request), request);
            }
        }

        CachedPage.onPeopleChanged(Utils.getOrg(request));
        return redirect(routes.CRM.viewTag(tag.getId()));
    }

    public Result undoTagChanges(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        Tag tag = Tag.findById(Integer.parseInt(values.get("tag_id")[0]), Utils.getOrg(request));

        String prefix = "tag-change-";
        for (String key_name : values.keySet()) {
            if (key_name.startsWith(prefix) && values.get(key_name)[0].equals("on")) {
                PersonTagChange change =
                    PersonTagChange.find.byId(Integer.parseInt(key_name.substring(prefix.length())));
                DB.execute(() -> {
                    if (change.getWasAdd()) {
                        tag.people.remove(change.getPerson());
                    } else {
                        if (!tag.people.contains(change.getPerson())) {
                            tag.people.add(change.getPerson());
                        }
                    }
                    tag.save();
                    change.delete();
                    notifyAboutTag(tag, change.getPerson(), !change.getWasAdd(), request);
                });
            }
        }

        CachedPage.onPeopleChanged(Utils.getOrg(request));
        return redirect(routes.CRM.viewTag(tag.getId()).url() + "#!history");
    }

    public Result viewTag(Integer id, Http.Request request) {
        Tag the_tag = Tag.find.query()
            .fetch("people")
            .fetch("people.phone_numbers", FetchConfig.ofQuery())
            .fetch("people.family", FetchConfig.ofQuery())
            .fetch("people.family.family_members", FetchConfig.ofQuery())
            .where().eq("organization", Utils.getOrg(request))
            .eq("id", id).findOne();

        List<Person> people = the_tag.people;

        Set<Person> people_with_family = new HashSet<>();
        for (Person p : people) {
            if (p.getFamily() != null) {
                people_with_family.addAll(p.getFamily().family_members);
            }
        }

        return ok(tag.render(the_tag, people, people_with_family, the_tag.getUseStudentDisplay(), true, request, mMessagesApi.preferred(request)));
    }

    private static void createCell(Row row, int j, Object value, CellStyle style) {
        Cell cell = row.createCell(j);
        if (value != null) {
            if (value instanceof String && ((String)value).trim().length() > 0) {
                cell.setCellValue(((String) value).trim());
            } else if (value instanceof Date) {
                cell.setCellValue((Date) value);
            }
        }
        if (style != null) {
            cell.setCellStyle(style);
        } else if (row.getRowStyle() != null) {
            cell.setCellStyle(row.getRowStyle());
        }
    }

    private static void createCell(Row row, int j, Object value) {
        createCell(row, j, value, null);
    }

    public Result downloadTag(Integer id, Http.Request request) throws IOException {
        Tag the_tag = Tag.find.query()
            .fetch("people")
            .fetch("people.phone_numbers", FetchConfig.ofQuery())
            .fetch("people.family", FetchConfig.ofQuery())
            .fetch("people.family.family_members", FetchConfig.ofQuery())
            .where().eq("organization", Utils.getOrg(request))
            .eq("id", id).findOne();

        List<Person> people = the_tag.people;

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("People");

        Row row = sheet.createRow(0);
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setFont(headerFont);
        int j = 0;
        createCell(row, j++, "First name", headerStyle);
        createCell(row, j++, "Last name", headerStyle);
        createCell(row, j++, "Family name", headerStyle);
        createCell(row, j++, "Address of", headerStyle);
        createCell(row, j++, "Display (JC) name", headerStyle);
        createCell(row, j++, "Gender", headerStyle);
        createCell(row, j++, "DOB", headerStyle);
        final int DOB_COLUMN = j - 1;
        createCell(row, j++, "Email", headerStyle);
        createCell(row, j++, "Phone 1", headerStyle);
        createCell(row, j++, "Phone 1 comment", headerStyle);
        createCell(row, j++, "Phone 2", headerStyle);
        createCell(row, j++, "Phone 2 comment", headerStyle);
        createCell(row, j++, "Phone 3", headerStyle);
        createCell(row, j++, "Phone 3 comment", headerStyle);
        createCell(row, j++, "Neighborhood", headerStyle);
        createCell(row, j++, "Address", headerStyle);
        createCell(row, j++, "City", headerStyle);
        createCell(row, j++, "State", headerStyle);
        createCell(row, j++, "ZIP", headerStyle);
        createCell(row, j++, "Notes", headerStyle);
        final int NOTES_COLUMN = j - 1;
        createCell(row, j++, "Previous school", headerStyle);
        createCell(row, j++, "School district", headerStyle);
        createCell(row, j++, "Grade", headerStyle);
        for (int j2 = 0; j2 < j; j2++) {
            sheet.autoSizeColumn(j);
        }

        CellStyle wordWrapStyle = wb.createCellStyle();
        wordWrapStyle.setWrapText(true);

        CellStyle dateStyle = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        dateStyle.setDataFormat(df.getFormat("m/d/yyyy"));

        CellStyle topBorderStyle = wb.createCellStyle();
        topBorderStyle.setBorderTop(BorderStyle.THICK);

        // Sort people who have multiple addresses at the end
        people.sort((p1, p2) -> {
            if (p1.hasMultipleAddresses() != p2.hasMultipleAddresses()) {
                return p1.hasMultipleAddresses() ? 1 : -1;
            }
            return p1.compareTo(p2);
        });

        int i = 1;
        boolean startedMultipleAddresses = false;
        for (Person p : people) {
            List<Person> address_people = new ArrayList<>();
            if (p.getFamily() == null || !p.getAddress().isEmpty()) {
                address_people.add(p);
            } else {
                boolean found_address = false;
                for (Person p2 : p.getFamily().family_members) {
                    if (!p2.getAddress().isEmpty()) {
                        address_people.add(p2);
                        found_address = true;
                    }
                }
                if (!found_address) {
                    address_people.add(p);
                }
            }
            for (Person address_person : address_people) {
                row = sheet.createRow(i);
                if (!startedMultipleAddresses && p.hasMultipleAddresses()) {
                    row.setRowStyle(topBorderStyle);
                    startedMultipleAddresses = true;
                }
                j = 0;
                createCell(row, j++, p.getFirstName());
                createCell(row, j++, p.getLastName());
                String family_name = null;
                if (p.getFamily() != null) {
                    family_name = p.getFamily().getFirstName() + " " + p.getFamily().getLastName();
                }
                if (family_name != null && family_name.trim().length() > 0) {
                    createCell(row, j++, family_name);
                } else {
                    j++;
                }
                if (p != address_person) {
                    createCell(row, j++, address_person.getFirstName() + " " + address_person.getLastName());
                } else {
                    j++;
                }
                createCell(row, j++, p.getDisplayName());
                createCell(row, j++, p.getGender());
                createCell(row, j++, p.getDob(), dateStyle);
                createCell(row, j++, p.getEmail());
                for (int n = 0; n < 3; n++) {
                    if (n < p.phone_numbers.size()) {
                        createCell(row, j++, p.phone_numbers.get(n).getNumber());
                        createCell(row, j++, p.phone_numbers.get(n).getComment());
                    } else {
                        j += 2;
                    }
                }

                createCell(row, j++, address_person.getNeighborhood());
                createCell(row, j++, address_person.getAddress());
                createCell(row, j++, address_person.getCity());
                createCell(row, j++, address_person.getState());
                createCell(row, j++, address_person.getZip());
                createCell(row, j++, p.getNotes(), wordWrapStyle);
                createCell(row, j++, p.getPreviousSchool());
                createCell(row, j++, p.getSchoolDistrict());
                createCell(row, j++, p.getGrade());
                i++;
            }
        }

        // Auto size all name columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(DOB_COLUMN);
        // Set "notes" column to 50 characters width
        sheet.setColumnWidth(NOTES_COLUMN, 256 * 50);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        baos.close();
        return ok(baos.toByteArray())
                .as("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .withHeader("Content-Disposition", "attachment; filename=" +
                the_tag.getTitle() + ".xlsx");

    }

    public Result newPerson(Http.Request request) {
        Form<Person> personForm = mFormFactory.form(Person.class);
        return ok(new_person.render(personForm, request, mMessagesApi.preferred(request)));
    }

    public Result makeNewPerson(Http.Request request) {
        Form<Person> personForm = mFormFactory.form(Person.class);
        Form<Person> filledForm = personForm.bindFromRequest(request);
        if(filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            return badRequest(
                new_person.render(filledForm, request, mMessagesApi.preferred(request))
            );
        } else {
            Person new_person = Person.create(filledForm, Utils.getOrg(request));
            return redirect(routes.CRM.person(new_person.getPersonId()));
        }
    }

    public Result deletePerson(Integer id) {
        Person.delete(id);
        return redirect(routes.CRM.recentComments());
    }

    public Result editPerson(Integer id, Http.Request request) {
        Person p = Person.findById(id, Utils.getOrg(request));
        return ok(edit_person.render(p.fillForm(), request, mMessagesApi.preferred(request)));
    }

    public Result savePersonEdits(Http.Request request) {
        CachedPage.onPeopleChanged(Utils.getOrg(request));

        Form<Person> personForm = mFormFactory.form(Person.class);
        Form<Person> filledForm = personForm.bindFromRequest(request);
        if(filledForm.hasErrors()) {
            System.out.println("ERRORS: " + filledForm.errorsAsJson().toString());
            for (ValidationError error : filledForm.errors()) {
                System.out.println(error.key() + ", " + error.message());
            }
            return badRequest(
                edit_person.render(filledForm, request, mMessagesApi.preferred(request))
            );
        }

        Person updatedPerson = Person.updateFromForm(filledForm, Utils.getOrg(request));
        return redirect(routes.CRM.person(
                (updatedPerson.getIsFamily() ? updatedPerson.family_members.get(0) : updatedPerson).getPersonId()));
    }

    public Result addComment(Http.Request request) {
        CachedPage.remove(CachedPage.RECENT_COMMENTS, Utils.getOrg(request));
        Form<Comment> filledForm = mFormFactory.form(Comment.class).bindFromRequest(request);
        Comment new_comment = new Comment();

        new_comment.setPerson(Person.findById(Integer.parseInt(filledForm.field("person").value().get()), Utils.getOrg(request)));
        new_comment.setUser(Application.getCurrentUser(request));
        new_comment.setMessage(filledForm.field("message").value().get());

        String task_id_string = filledForm.field("comment_task_ids").value().get();

        if (task_id_string.length() > 0 || new_comment.getMessage().length() > 0) {
            new_comment.save();

            String[] task_ids = task_id_string.split(",");
            for (String id_string : task_ids) {
                if (!id_string.isEmpty()) {
                    int id = Integer.parseInt(id_string);
                    if (id >= 1) {
                        CompletedTask.create(Task.findById(id, Utils.getOrg(request)), new_comment);
                    }
                }
            }

            if (filledForm.field("send_email").value().isPresent()) {
                for (NotificationRule rule :
                        NotificationRule.findByType(NotificationRule.TYPE_COMMENT, Utils.getOrg(request))) {
                    play.libs.mailer.Email mail = new play.libs.mailer.Email();
                    mail.setSubject("DemSchoolTools comment: " + new_comment.getUser().getName() + " & " + new_comment.getPerson().getInitials());
                    mail.addTo(rule.getEmail());
                    mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
                    mail.setBodyHtml(comment_email.render(Comment.find.byId(new_comment.getId()), request, mMessagesApi.preferred(request)).toString());
                    sMailer.send(mail);
                }
            }

            return ok(comment_fragment.render(Comment.find.byId(new_comment.getId()), false, request, mMessagesApi.preferred(request)));
        } else {
            return ok();
        }
    }

    public static int calcAgeAtBeginningOfSchool(Person p) {
        if (p.getDob() == null) {
            return -1;
        }
        return (int)((ModelUtils.getStartOfYear().getTime() - p.getDob().getTime()) / 1000 / 60 / 60 / 24 / 365.25);
    }

    public static String formatDob(Date d, OrgConfig orgConfig) {
        if (d == null) {
            return "---";
        }
        if (orgConfig.euro_dates) {
            return new SimpleDateFormat("dd/MM/yy").format(d);
        }
        return new SimpleDateFormat("MM/dd/yy").format(d);
    }

    public static String formatDate(Date d, OrgConfig orgConfig) {
        d = new Date(d.getTime() + orgConfig.time_zone.getOffset(d.getTime()));
        Date now = Utils.localNow(orgConfig);

        long diffHours = (now.getTime() - d.getTime()) / 1000 / 60 / 60;

        // String format = "EEE MMMM d, h:mm a";
        String format;

        if (diffHours < 24) {
            format = "h:mm a";
        } else if (diffHours < 24 * 7) {
            format = "EEEE, MMMM d";
        } else {
            if (orgConfig.euro_dates) {
                format = "dd/MM/yy";
            } else {
                format = "MM/dd/yy";
            }
        }
        return new SimpleDateFormat(format).format(d);
    }

    public Result viewTaskList(Integer id, Http.Request request) {
        TaskList list = TaskList.findById(id, Utils.getOrg(request));
        List<Person> people = list.getTag().people;

        return ok(task_list.render(list, people, request, mMessagesApi.preferred(request)));
    }
}
