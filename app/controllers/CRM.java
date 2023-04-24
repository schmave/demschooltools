package controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import io.ebean.*;
import com.ecwid.mailchimp.*;
import com.ecwid.mailchimp.method.v2_0.lists.ListMethodResult;
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
                "recent_comments", Organization.getByHost(request)) {
                @Override
                String render() {
                    List<Comment> recent_comments = Comment.find.query()
                        .fetch("person")
                        .fetch("completed_tasks", FetchConfig.ofQuery())
                        .fetch("completed_tasks.task", FetchConfig.ofQuery())
                        .where().eq("person.organization", Organization.getByHost(request))
                        .orderBy("created DESC").setMaxRows(20).findList();

                    return people_index.render(recent_comments, request, mMessagesApi.preferred(request)).toString();
                }
            }, request, mMessagesApi.preferred(request)));
    }

    public Result person(Integer id, Http.Request request) {
        Person the_person = Person.findById(id, Organization.getByHost(request));

        List<Person> family_members =
            Person.find.query().where().isNotNull("family").eq("family", the_person.family).
                ne("person_id", the_person.person_id).findList();

        Set<Integer> family_ids = new HashSet<>();
        family_ids.add(the_person.person_id);
        for (Person family : family_members) {
            family_ids.add(family.person_id);
        }

        List<Comment> all_comments = Comment.find.query().where().in("person_id", family_ids).
            order("created DESC").findList();

        String comment_destination = "<No email set>";
        boolean first = true;
        for (NotificationRule rule :
                NotificationRule.findByType(NotificationRule.TYPE_COMMENT, Organization.getByHost(request))) {
            if (first) {
                comment_destination = rule.email;
                first = false;
            } else {
                comment_destination += ", " + rule.email;
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
                    Expr.or(Expr.ilike("last_name", "%" + term + "%"),
                            Expr.ilike("first_name", "%" + term + "%"));
            this_expr = Expr.or(this_expr,
                    Expr.ilike("address", "%" + term + "%"));
            this_expr = Expr.or(this_expr,
                    Expr.ilike("email", "%" + term + "%"));
            this_expr = Expr.or(this_expr,
                Expr.ilike("display_name", "%" + term + "%"));

            people_matched_this_round =
                    Person.find.query().where().add(this_expr)
                            .eq("organization", Organization.getByHost(request))
                            .eq("is_family", false)
                    .findList();

            List<PhoneNumber> phone_numbers =
                    PhoneNumber.find.query().where().ilike("number", "%" + term + "%")
                    .eq("owner.organization", Organization.getByHost(request))
                    .findList();
            for (PhoneNumber pn : phone_numbers) {
                people_matched_this_round.add(pn.owner);
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
            String label = p.first_name;
            if (p.last_name != null) {
                label = label + " " + p.last_name;
            }
            if (p.display_name != null && !p.display_name.equals("")) {
                label += " (\"" + p.display_name + "\")";
            }
            values.put("label", label);
            values.put("id", "" + p.person_id);
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
                .eq("organization", Organization.getByHost(request))
                .eq("show_in_menu", true)
                .ilike("title", "%" + term + "%").findList();

        List<Tag> existing_tags = null;
        if (personId >= 0) {
            Person p = Person.findById(personId, Organization.getByHost(request));
            if (p != null) {
                existing_tags = p.tags;
            }
        }

        List<Map<String, String> > result = new ArrayList<>();
        for (Tag t : selected_tags) {
            if (existing_tags == null || !existing_tags.contains(t)) {
                HashMap<String, String> values = new HashMap<>();
                values.put("label", t.title);
                values.put("id", "" + t.id);
                result.add(values);
            }
        }

        boolean tag_already_exists = false;
        for (Tag t : selected_tags) {
            if (t.title.equalsIgnoreCase(term)) {
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
                if (p.family != null) {
                    selected_people.addAll(p.family.family_members);
                }
            }
        }

        if (familyMode.equals("family_no_kids")) {
            Set<Person> no_kids = new HashSet<>();
            for (Person p2 : selected_people) {
                if (p2.dob == null ||
                        CRM.calcAge(p2) > 18) {
                    no_kids.add(p2);
                }
            }
            selected_people = no_kids;
        }

        return selected_people;
    }

    public Result renderTagMembers(Integer tagId, String familyMode, Http.Request request) {
        Organization org = Organization.getByHost(request);
        Tag the_tag = Tag.findById(tagId, org);
        return ok(to_address_fragment.render(the_tag.title,
                getTagMembers(tagId, familyMode, org), request, mMessagesApi.preferred(request)));
    }

    public Result addTag(Integer tagId, String title, Integer personId, Http.Request request) {
        Organization org = Organization.getByHost(request);
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

    public Result removeTag(Integer person_id, Integer tag_id, Http.Request request) {
        Tag t = Tag.findById(tag_id, Organization.getByHost(request));
        Person p = Person.findById(person_id, Organization.getByHost(request));

        removeTag(t, p, Application.getCurrentUser(request), request);
        CachedPage.onPeopleChanged(Organization.getByHost(request));

        return ok();
    }

    public void removeTag(Tag tag, Person person, User current_user, Http.Request request) {
        if (!tag.people.contains(person)) {
            return;
        }
        DB.execute(() -> {
            if (DB.sqlUpdate("DELETE from person_tag where person_id=" + person.person_id +
                    " AND tag_id=" + tag.id).execute() == 1) {
                PersonTagChange.create(
                        tag,
                        person,
                        current_user,
                        false);
            }
            notifyAboutTag(tag, person, false, request);
        });
    }

    public void notifyAboutTag(Tag t, Person p, boolean was_add, Http.Request request) {
        for (NotificationRule rule : t.notification_rules) {
            play.libs.mailer.Email mail = new play.libs.mailer.Email();
            if (was_add) {
                mail.setSubject(p.getInitials() + " added to tag " + t.title);
            } else {
                mail.setSubject(p.getInitials() + " removed from tag " + t.title);
            }
            mail.addTo(rule.email);
            mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
            mail.setBodyHtml(tag_email.render(t,
                    Application.getCurrentUser(request).name,
                    p, was_add, request, mMessagesApi.preferred(request)).toString());
            sMailer.send(mail);
        }
    }

    public Result allPeople(Http.Request request) {
        return ok(all_people.render(Person.all(Organization.getByHost(request)), request, mMessagesApi.preferred(request)));
    }

    public Result viewAllTags(Http.Request request) {
        List<Tag> tags = Tag.find.query()
            .where().eq("organization", Organization.getByHost(request))
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
        Form<Tag> filled_form = mFormFactory.form(Tag.class).fill(Tag.findById(id, Organization.getByHost(request)));
        return ok(edit_tag.render(filled_form, request, mMessagesApi.preferred(request)));
    }

    public Result saveTag(Http.Request request) {
        Form<Tag> form = mFormFactory.form(Tag.class).bindFromRequest(request);

        if (form.field("id").value().isPresent()) {
            Tag t = Tag.findById(Integer.parseInt(form.field("id").value().get()), Organization.getByHost(request));
            t.updateFromForm(form);

            CachedPage.onPeopleChanged(Organization.getByHost(request));

            return redirect(routes.CRM.viewTag(t.id));
        }
        return redirect(routes.CRM.recentComments());
    }

    public Result addPeopleFromTag(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        Tag dest_tag = Tag.findById(Integer.parseInt(values.get("dest_id")[0]), Organization.getByHost(request));
        Tag src_tag = Tag.findById(Integer.parseInt(values.get("tag_id")[0]), Organization.getByHost(request));

        for (Person p : src_tag.people) {
            addTag(dest_tag, p, Application.getCurrentUser(request), request);
        }

        CachedPage.onPeopleChanged(Organization.getByHost(request));
        return redirect(routes.CRM.viewTag(dest_tag.id));
    }

    public Result addPeopleToTag(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        Tag dest_tag = Tag.findById(Integer.parseInt(values.get("dest_id")[0]), Organization.getByHost(request));

        for (String person_id_str : values.get("person_id")) {
            Person p = Person.findById(Integer.parseInt(person_id_str), Organization.getByHost(request));
            addTag(dest_tag, p, Application.getCurrentUser(request), request);
        }

        CachedPage.onPeopleChanged(Organization.getByHost(request));
        return redirect(routes.CRM.viewTag(dest_tag.id));
    }

    public Result removePeopleFromTag(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        Tag tag = Tag.findById(Integer.parseInt(values.get("tag_id")[0]), Organization.getByHost(request));

        String prefix = "person-";
        for (String key_name : values.keySet()) {
            if (key_name.startsWith(prefix) && values.get(key_name)[0].equals("on")) {
                Person p = Person.findById(Integer.parseInt(key_name.substring(prefix.length())), Organization.getByHost(request));
                removeTag(tag, p, Application.getCurrentUser(request), request);
            }
        }

        CachedPage.onPeopleChanged(Organization.getByHost(request));
        return redirect(routes.CRM.viewTag(tag.id));
    }

    public Result undoTagChanges(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        Tag tag = Tag.findById(Integer.parseInt(values.get("tag_id")[0]), Organization.getByHost(request));

        String prefix = "tag-change-";
        for (String key_name : values.keySet()) {
            if (key_name.startsWith(prefix) && values.get(key_name)[0].equals("on")) {
                PersonTagChange change =
                    PersonTagChange.find.byId(Integer.parseInt(key_name.substring(prefix.length())));
                DB.execute(() -> {
                    if (change.was_add) {
                        tag.people.remove(change.person);
                    } else {
                        if (!tag.people.contains(change.person)) {
                            tag.people.add(change.person);
                        }
                    }
                    tag.save();
                    change.delete();
                    notifyAboutTag(tag, change.person, !change.was_add, request);
                });
            }
        }

        CachedPage.onPeopleChanged(Organization.getByHost(request));
        return redirect(routes.CRM.viewTag(tag.id).url() + "#!history");
    }

    public Result viewTag(Integer id, Http.Request request) {
        Tag the_tag = Tag.find.query()
            .fetch("people")
            .fetch("people.phone_numbers", FetchConfig.ofQuery())
            .fetch("people.family", FetchConfig.ofQuery())
            .fetch("people.family.family_members", FetchConfig.ofQuery())
            .where().eq("organization", Organization.getByHost(request))
            .eq("id", id).findOne();

        List<Person> people = the_tag.people;

        Set<Person> people_with_family = new HashSet<>();
        for (Person p : people) {
            if (p.family != null) {
                people_with_family.addAll(p.family.family_members);
            }
        }

        return ok(tag.render(the_tag, people, people_with_family, the_tag.use_student_display, true, request, mMessagesApi.preferred(request)));
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
            .where().eq("organization", Organization.getByHost(request))
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
            if (p.family == null || !p.address.isEmpty()) {
                address_people.add(p);
            } else {
                boolean found_address = false;
                for (Person p2 : p.family.family_members) {
                    if (!p2.address.isEmpty()) {
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
                createCell(row, j++, p.first_name);
                createCell(row, j++, p.last_name);
                String family_name = null;
                if (p.family != null) {
                    family_name = p.family.first_name + " " + p.family.last_name;
                }
                if (family_name != null && family_name.trim().length() > 0) {
                    createCell(row, j++, family_name);
                } else {
                    j++;
                }
                if (p != address_person) {
                    createCell(row, j++, address_person.first_name + " " + address_person.last_name);
                } else {
                    j++;
                }
                createCell(row, j++, p.getDisplayName());
                createCell(row, j++, p.gender);
                createCell(row, j++, p.dob, dateStyle);
                createCell(row, j++, p.email);
                for (int n = 0; n < 3; n++) {
                    if (n < p.phone_numbers.size()) {
                        createCell(row, j++, p.phone_numbers.get(n).number);
                        createCell(row, j++, p.phone_numbers.get(n).comment);
                    } else {
                        j += 2;
                    }
                }

                createCell(row, j++, address_person.neighborhood);
                createCell(row, j++, address_person.address);
                createCell(row, j++, address_person.city);
                createCell(row, j++, address_person.state);
                createCell(row, j++, address_person.zip);
                createCell(row, j++, p.notes, wordWrapStyle);
                createCell(row, j++, p.previous_school);
                createCell(row, j++, p.school_district);
                createCell(row, j++, p.grade);
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
                .withHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .withHeader("Content-Disposition", "attachment; filename=" +
                the_tag.title + ".xlsx");

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
            Person new_person = Person.create(filledForm, Organization.getByHost(request));
            return redirect(routes.CRM.person(new_person.person_id));
        }
    }

    public Result viewPendingEmail(Http.Request request) {
        Email e =Email.find.query().where()
                .eq("organization", Organization.getByHost(request))
                .eq("deleted", false).eq("sent", false).orderBy("id ASC").setMaxRows(1).findOne();

        if (e == null) {
            return redirect(routes.CRM.recentComments());
        }

        Tag staff_tag = Tag.find.query().where()
            .eq("organization", Organization.getByHost(request))
            .eq("title", "Staff").findOne();
        List<Person> people = staff_tag.people;

        ArrayList<String> test_addresses = new ArrayList<>();
        for (Person p : people) {
            test_addresses.add(p.email);
        }
        test_addresses.add("staff@threeriversvillageschool.org");

        ArrayList<String> from_addresses = new ArrayList<>();
        from_addresses.add("office@threeriversvillageschool.org");
        from_addresses.add("evan@threeriversvillageschool.org");
        from_addresses.add("jmp@threeriversvillageschool.org");
        from_addresses.add("jancey@threeriversvillageschool.org");
        from_addresses.add("info@threeriversvillageschool.org");
        from_addresses.add("staff@threeriversvillageschool.org");

        e.parseMessage();
        return ok(view_pending_email.render(e, test_addresses, from_addresses, request, mMessagesApi.preferred(request)));
    }

    public Result sendTestEmail(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        Email e = Email.findById(Integer.parseInt(values.get("id")[0]), Organization.getByHost(request));
        e.parseMessage();

        try {
            MimeMessage to_send = new MimeMessage(e.parsedMessage);
            to_send.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(values.get("dest_email")[0]));
            to_send.setFrom(new InternetAddress("Papal DB <noreply@threeriversvillageschool.org>"));
            Transport.send(to_send);
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }

        return ok();
    }

    public Result sendEmail(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        Email e = Email.findById(Integer.parseInt(values.get("id")[0]), Organization.getByHost(request));
        e.parseMessage();

        int tagId = Integer.parseInt(values.get("tagId")[0]);
        Tag theTag = Tag.findById(tagId, Organization.getByHost(request));
        String familyMode = values.get("familyMode")[0];
        Collection<Person> recipients = getTagMembers(tagId, familyMode, Organization.getByHost(request));

        boolean hadErrors = false;

        for (Person p : recipients) {
            if (p.email != null && !p.email.equals("")) {
                try {
                    MimeMessage to_send = new MimeMessage(e.parsedMessage);
                    to_send.addRecipient(Message.RecipientType.TO,
                            new InternetAddress(p.email, p.first_name + " " + p.last_name));
                    to_send.setFrom(new InternetAddress(values.get("from")[0]));
                    Transport.send(to_send);
                } catch (MessagingException | UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                    hadErrors = true;
                }
            }
        }

        // Send confirmation email
        try {
            MimeMessage to_send = new MimeMessage(e.parsedMessage);
            to_send.addRecipient(Message.RecipientType.TO,
                    new InternetAddress("Staff <staff@threeriversvillageschool.org>"));
            String subject = "(Sent to " + theTag.title + " " + familyMode + ") " + to_send.getSubject();
            if (hadErrors) {
                subject = "***ERRORS*** " + subject;
            }
            to_send.setSubject(subject);
            to_send.setFrom(new InternetAddress(values.get("from")[0]));
            Transport.send(to_send);
        } catch (MessagingException ex) {
            ex.printStackTrace();
        }

        e.delete();
        return ok();
    }

    public Result deleteEmail(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();
        Email e = Email.findById(Integer.parseInt(values.get("id")[0]), Organization.getByHost(request));
        e.delete();

        return ok();
    }

    public Result deletePerson(Integer id) {
        Person.delete(id);
        return redirect(routes.CRM.recentComments());
    }

    public Result editPerson(Integer id, Http.Request request) {
        Person p = Person.findById(id, Organization.getByHost(request));
        return ok(edit_person.render(p.fillForm(), request, mMessagesApi.preferred(request)));
    }

    public Result savePersonEdits(Http.Request request) {
        CachedPage.onPeopleChanged(Organization.getByHost(request));

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

        Person updatedPerson = Person.updateFromForm(filledForm, Organization.getByHost(request));
        return redirect(routes.CRM.person(
                (updatedPerson.is_family ? updatedPerson.family_members.get(0) : updatedPerson).person_id));
    }

    public Result addComment(Http.Request request) {
        CachedPage.remove(CachedPage.RECENT_COMMENTS, Organization.getByHost(request));
        Form<Comment> filledForm = mFormFactory.form(Comment.class).bindFromRequest(request);
        Comment new_comment = new Comment();

        new_comment.person = Person.findById(Integer.parseInt(filledForm.field("person").value().get()), Organization.getByHost(request));
        new_comment.user = Application.getCurrentUser(request);
        new_comment.message = filledForm.field("message").value().get();

        String task_id_string = filledForm.field("comment_task_ids").value().get();

        if (task_id_string.length() > 0 || new_comment.message.length() > 0) {
            new_comment.save();

            String[] task_ids = task_id_string.split(",");
            for (String id_string : task_ids) {
                if (!id_string.isEmpty()) {
                    int id = Integer.parseInt(id_string);
                    if (id >= 1) {
                        CompletedTask.create(Task.findById(id, Organization.getByHost(request)), new_comment);
                    }
                }
            }

            if (filledForm.field("send_email").value().isPresent()) {
                for (NotificationRule rule :
                        NotificationRule.findByType(NotificationRule.TYPE_COMMENT, Organization.getByHost(request))) {
                    play.libs.mailer.Email mail = new play.libs.mailer.Email();
                    mail.setSubject("DemSchoolTools comment: " + new_comment.user.name + " & " + new_comment.person.getInitials());
                    mail.addTo(rule.email);
                    mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
                    mail.setBodyHtml(comment_email.render(Comment.find.byId(new_comment.id), request, mMessagesApi.preferred(request)).toString());
                    sMailer.send(mail);
                }
            }

            return ok(comment_fragment.render(Comment.find.byId(new_comment.id), false, request, mMessagesApi.preferred(request)));
        } else {
            return ok();
        }
    }

    public static int calcAge(Person p) {
        return (int)((new Date().getTime() - p.dob.getTime()) / 1000 / 60 / 60 / 24 / 365.25);
    }

    public static int calcAgeAtBeginningOfSchool(Person p) {
        if (p.dob == null) {
            return -1;
        }
        return (int)((Application.getStartOfYear().getTime() - p.dob.getTime()) / 1000 / 60 / 60 / 24 / 365.25);
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
        TaskList list = TaskList.findById(id, Organization.getByHost(request));
        List<Person> people = list.tag.people;

        return ok(task_list.render(list, people, request, mMessagesApi.preferred(request)));
    }

    public Result viewMailchimpSettings(Http.Request request) {
        MailChimpClient mailChimpClient = new MailChimpClient();
        Organization org = Organization.getByHost(request);

        Map<String, ListMethodResult.Data> mc_list_map =
            Public.getMailChimpLists(mailChimpClient, org.mailchimp_api_key);

        return ok(view_mailchimp_settings.render(mFormFactory.form(Organization.class), org,
            MailchimpSync.find.query().where().eq("tag.organization", Organization.getByHost(request)).findList(),
            mc_list_map, request, mMessagesApi.preferred(request)));
    }

    public Result saveMailchimpSettings(Http.Request request) {
        final Map<String, String[]> values = request.body().asFormUrlEncoded();

        if (values.containsKey("mailchimp_api_key")) {
            Organization.getByHost(request).setMailChimpApiKey(values.get("mailchimp_api_key")[0]);
        }

        if (values.containsKey("mailchimp_updates_email")) {
            Organization.getByHost(request).setMailChimpUpdatesEmail(values.get("mailchimp_updates_email")[0]);
        }

        if (values.containsKey("sync_type")) {
            for (String tag_id : values.get("tag_id")) {
                Tag t = Tag.findById(Integer.parseInt(tag_id), Organization.getByHost(request));
                MailchimpSync.create(t,
                    values.get("mailchimp_list_id")[0],
                    values.get("sync_type")[0].equals("local_add"),
                    values.get("sync_type")[0].equals("local_remove"));
            }
        }

        if (values.containsKey("remove_sync_id")) {
            MailchimpSync sync = MailchimpSync.find.byId(Integer.parseInt(
                values.get("remove_sync_id")[0]));
            sync.delete();
        }

        return redirect(routes.CRM.viewMailchimpSettings());
    }
}