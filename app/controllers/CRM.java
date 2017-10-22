package controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import com.avaje.ebean.*;
import com.ecwid.mailchimp.*;
import com.ecwid.mailchimp.method.v2_0.lists.ListMethodResult;
import com.google.inject.Inject;

import models.*;

import models.Comment;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import play.data.*;
import play.data.validation.ValidationError;
import play.libs.Json;
import play.mvc.*;


@With(DumpOnError.class)
@Secured.Auth(UserRole.ROLE_ALL_ACCESS)
public class CRM extends Controller {

    Application mApplication;

    @Inject
    public CRM(final Application app) {
        mApplication = app;
    }

    public Result recentComments() {
        return ok(views.html.cached_page.render(
            new CachedPage(CachedPage.RECENT_COMMENTS,
                "All people",
                "crm",
                "recent_comments") {
                @Override
                String render() {
                    List<Comment> recent_comments = Comment.find
                        .fetch("person")
                        .fetch("completed_tasks", new FetchConfig().query())
                        .fetch("completed_tasks.task", new FetchConfig().query())
                        .where().eq("person.organization", Organization.getByHost())
                        .orderBy("created DESC").setMaxRows(20).findList();

                    return views.html.people_index.render(recent_comments).toString();
                }
            }));
    }

    public Result person(Integer id) {
        Person the_person = Person.findById(id);

        List<Person> family_members =
            Person.find.where().isNotNull("family").eq("family", the_person.family).
                ne("person_id", the_person.person_id).findList();

        Set<Integer> family_ids = new HashSet<Integer>();
        family_ids.add(the_person.person_id);
        for (Person family : family_members) {
            family_ids.add(family.person_id);
        }

        List<Comment> all_comments = Comment.find.where().in("person_id", family_ids).
            order("created DESC").findList();

        String comment_destination = "<No email set>";
        boolean first = true;
        for (NotificationRule rule :
                NotificationRule.findByType(NotificationRule.TYPE_COMMENT)) {
            if (first) {
                comment_destination = rule.email;
                first = false;
            } else {
                comment_destination += ", " + rule.email;
            }
        }


        return ok(views.html.family.render(
            the_person,
            family_members,
            all_comments,
            comment_destination));
    }

    public Result jsonPeople(String query) {
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

			people_matched_this_round =
				Person.find.where().add(this_expr)
					.eq("organization", Organization.getByHost())
					.eq("is_family", false)
                    .findList();

			List<PhoneNumber> phone_numbers =
				PhoneNumber.find.where().ilike("number", "%" + term + "%")
                    .eq("owner.organization", Organization.getByHost())
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

		List<Person> sorted_people = new ArrayList<>();
		sorted_people.addAll(selected_people);
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
            values.put("label", label);
            values.put("id", "" + p.person_id);
            result.add(values);
        }

        return ok(Json.stringify(Json.toJson(result)));
    }

    // personId should be -1 if the client doesn't care about a particular
    // person, but just wants a list of available tags. In that case,
    // the "create new tag" functionality is also disabled.
    public Result jsonTags(String term, Integer personId) {
        String like_arg = "%" + term + "%";
        List<Tag> selected_tags =
            Tag.find.where()
                .eq("organization", Organization.getByHost())
                .eq("show_in_menu", true)
                .ilike("title", "%" + term + "%").findList();

		List<Tag> existing_tags = null;
        if (personId >= 0) {
            Person p = Person.findById(personId);
            if (p != null) {
                existing_tags = p.tags;
            }
        }

        List<Map<String, String> > result = new ArrayList<Map<String, String> > ();
        for (Tag t : selected_tags) {
            if (existing_tags == null || !existing_tags.contains(t)) {
                HashMap<String, String> values = new HashMap<String, String>();
                values.put("label", t.title);
                values.put("id", "" + t.id);
                result.add(values);
            }
        }

        boolean tag_already_exists = false;
        for (Tag t : selected_tags) {
            if (t.title.toLowerCase().equals(term.toLowerCase())) {
                tag_already_exists = true;
            }
        }

        if (personId >= 0 && !tag_already_exists) {
            HashMap<String, String> values = new HashMap<String, String>();
            values.put("label", "Create new tag: " + term);
            values.put("id", "-1");
            result.add(values);
        }

        return ok(Json.stringify(Json.toJson(result)));
    }

	public Collection<Person> getTagMembers(Integer tagId, String familyMode) {
        List<Person> people = Tag.findById(tagId).people;

        Set<Person> selected_people = new HashSet<Person>();

		selected_people.addAll(people);

		if (!familyMode.equals("just_tags")) {
			for (Person p : people) {
				if (p.family != null) {
					selected_people.addAll(p.family.family_members);
				}
			}
		}

		if (familyMode.equals("family_no_kids")) {
			Set<Person> no_kids = new HashSet<Person>();
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

	public Result renderTagMembers(Integer tagId, String familyMode) {
        Tag the_tag = Tag.findById(tagId);
		return ok(views.html.to_address_fragment.render(the_tag.title,
			getTagMembers(tagId, familyMode)));
	}

    public Result addTag(Integer tagId, String title, Integer personId) {
        Person p = Person.findById(personId);
        if (p == null) {
            return badRequest();
        }

        Tag the_tag;
        if (tagId == null) {
            the_tag = Tag.create(title);
        } else {
            the_tag = Tag.find.ref(tagId);
        }

        addTag(the_tag, p);
        CachedPage.onPeopleChanged();
        return ok(views.html.tag_fragment.render(the_tag, p));
    }

    public void addTag(Tag tag, Person person) {
        if (tag.people.contains(person)) {
            return;
        }

        Ebean.execute(new TxRunnable() {
            @Override
            public void run() {
                tag.people.add(person);
                tag.save();

                PersonTagChange ptc = PersonTagChange.create(
                        tag,
                        person,
                        mApplication.getCurrentUser(),
                        true);

                person.tags.add(tag);
                notifyAboutTag(tag, person, true);
            }
        });
    }

    public Result removeTag(Integer person_id, Integer tag_id) {
        Tag t = Tag.findById(tag_id);
        Person p = Person.findById(person_id);

        removeTag(t, p);
        CachedPage.onPeopleChanged();

        return ok();
    }

    public void removeTag(Tag tag, Person person) {
        if (!tag.people.contains(person)) {
            return;
        }
        Ebean.execute(new TxRunnable() {
            @Override
            public void run() {
                if (Ebean.createSqlUpdate("DELETE from person_tag where person_id=" + person.person_id +
                        " AND tag_id=" + tag.id).execute() == 1) {
                    PersonTagChange ptc = PersonTagChange.create(
                            tag,
                            person,
                            mApplication.getCurrentUser(),
                            false);
                }
                notifyAboutTag(tag, person, false);
            }
        });
    }

    public static void notifyAboutTag(Tag t, Person p, boolean was_add) {
        for (NotificationRule rule : t.notification_rules) {
            play.libs.mailer.Email mail = new play.libs.mailer.Email();
            if (was_add) {
                mail.setSubject(getInitials(p) + " added to tag " + t.title);
            } else {
                mail.setSubject(getInitials(p) + " removed from tag " + t.title);
            }
            mail.addTo(rule.email);
            mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
            mail.setBodyHtml(views.html.tag_email.render(t, p, was_add).toString());
            play.libs.mailer.MailerPlugin.send(mail);
        }
    }

    public Result allPeople() {
        return ok(views.html.all_people.render(Person.all()));
    }

    public Result viewAllTags() {
        List<Tag> tags = Tag.find
            .where().eq("organization", Organization.getByHost())
            .orderBy("lower(title)")
            .findList();

        Map<String, Object> scopes = new HashMap<>();
        List<Object> serialized_tags = new ArrayList<>();
        for (Tag tag : tags) {
            serialized_tags.add(tag.serialize());
        }
        scopes.put("tags", serialized_tags);

        return ok(views.html.main_with_mustache.render(
            "All Tags",
            "crm",
            "",
            "view_all_tags.html",
            scopes));
    }

    public Result editTag(Integer id) {
        Form<Tag> filled_form = Form.form(Tag.class).fill(Tag.findById(id));
        return ok(views.html.edit_tag.render(filled_form));
    }

    public Result saveTag() {
        Form<Tag> form = Form.form(Tag.class).bindFromRequest();

        if (form.field("id").value() != null) {
            Tag t = Tag.findById(Integer.parseInt(form.field("id").value()));
            t.updateFromForm(form);

            CachedPage.onPeopleChanged();

            return redirect(routes.CRM.viewTag(t.id));
        }
        return redirect(routes.CRM.recentComments());
    }

    public Result addPeopleFromTag() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        Tag dest_tag = Tag.findById(Integer.parseInt(values.get("dest_id")[0]));
        Tag src_tag = Tag.findById(Integer.parseInt(values.get("tag_id")[0]));

        for (Person p : src_tag.people) {
            addTag(dest_tag, p);
        }

        CachedPage.onPeopleChanged();
        return redirect(routes.CRM.viewTag(dest_tag.id));
    }

    public Result addPeopleToTag() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        Tag dest_tag = Tag.findById(Integer.parseInt(values.get("dest_id")[0]));

        for (String person_id_str : values.get("person_id")) {
            Person p = Person.findById(Integer.parseInt(person_id_str));
            addTag(dest_tag, p);
        }

        CachedPage.onPeopleChanged();
        return redirect(routes.CRM.viewTag(dest_tag.id));
    }

    public Result removePeopleFromTag() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        Tag tag = Tag.findById(Integer.parseInt(values.get("tag_id")[0]));

        String prefix = "person-";
        for (String key_name : values.keySet()) {
            if (key_name.startsWith(prefix) && values.get(key_name)[0].equals("on")) {
                Person p = Person.findById(Integer.parseInt(key_name.substring(prefix.length())));
                removeTag(tag, p);
            }
        }

        CachedPage.onPeopleChanged();
        return redirect(routes.CRM.viewTag(tag.id));
    }

    public Result undoTagChanges() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        Tag tag = Tag.findById(Integer.parseInt(values.get("tag_id")[0]));

        String prefix = "tag-change-";
        for (String key_name : values.keySet()) {
            if (key_name.startsWith(prefix) && values.get(key_name)[0].equals("on")) {
                PersonTagChange change =
                    PersonTagChange.find.byId(Integer.parseInt(key_name.substring(prefix.length())));
                Ebean.execute(new TxRunnable() {
                    @Override
                    public void run() {
                        if (change.was_add) {
                            tag.people.remove(change.person);
                        } else {
                            if (!tag.people.contains(change.person)) {
                                tag.people.add(change.person);
                            }
                        }
                        tag.save();
                        change.delete();
                        notifyAboutTag(tag, change.person, !change.was_add);
                    }
                });
            }
        }

        CachedPage.onPeopleChanged();
        return redirect(routes.CRM.viewTag(tag.id).url() + "#!history");
    }

    public Result viewTag(Integer id) {
        Tag the_tag = Tag.find
            .fetch("people")
            .fetch("people.phone_numbers", new FetchConfig().query())
            .fetch("people.family", new FetchConfig().query())
            .fetch("people.family.family_members", new FetchConfig().query())
            .where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();

        List<Person> people = the_tag.people;

        Set<Person> people_with_family = new HashSet<Person>();
        for (Person p : people) {
            if (p.family != null) {
                people_with_family.addAll(p.family.family_members);
            }
        }

        return ok(views.html.tag.render(
            the_tag, people, people_with_family, the_tag.use_student_display, true));
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

    public Result downloadTag(Integer id) throws IOException {
        Tag the_tag = Tag.find
            .fetch("people")
            .fetch("people.phone_numbers", new FetchConfig().query())
            .fetch("people.family", new FetchConfig().query())
            .fetch("people.family.family_members", new FetchConfig().query())
            .where().eq("organization", Organization.getByHost())
            .eq("id", id).findUnique();

        response().setHeader("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response().setHeader("Content-Disposition", "attachment; filename=" +
            the_tag.title + ".xlsx");

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
        people.sort(new Comparator<Person>() {
            @Override
            public int compare(Person p1, Person p2) {
                if (p1.hasMultipleAddresses() != p2.hasMultipleAddresses()) {
                    return p1.hasMultipleAddresses() ? 1 : -1;
                }
                return p1.compareTo(p2);
            }
        });

        int i = 1;
        boolean startedMultipleAddresses = false;
        for (Person p : people) {
            List<Person> address_people = new ArrayList<>();
            if (p.family == null || !p.address.isEmpty()) {
                address_people.add(p);
            } else {
                for (Person p2 : p.family.family_members) {
                    if (!p2.address.isEmpty()) {
                        address_people.add(p2);
                    }
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
        return ok(baos.toByteArray());
    }

    public Result newPerson() {
        Form<Person> personForm = Form.form(Person.class);
        return ok(views.html.new_person.render(personForm));
    }

    public Result makeNewPerson() {
        Form<Person> personForm = Form.form(Person.class);
        Form<Person> filledForm = personForm.bindFromRequest();
        if(filledForm.hasErrors()) {
            return badRequest(
                views.html.new_person.render(filledForm)
            );
        } else {
            Person new_person = Person.create(filledForm);
            return redirect(routes.CRM.person(new_person.person_id));
        }
    }

	static Email getPendingEmail() {
		return Email.find.where()
            .eq("organization", Organization.getByHost())
            .eq("deleted", false).eq("sent", false).orderBy("id ASC").setMaxRows(1).findUnique();
	}

	public static boolean hasPendingEmail() {
		return getPendingEmail() != null;
	}

	public Result viewPendingEmail() {
		Email e = getPendingEmail();
		if (e == null) {
			return redirect(routes.CRM.recentComments());
		}

        Tag staff_tag = Tag.find.where()
            .eq("organization", Organization.getByHost())
            .eq("title", "Staff").findUnique();
        List<Person> people = staff_tag.people;

        ArrayList<String> test_addresses = new ArrayList<String>();
        for (Person p : people) {
            test_addresses.add(p.email);
        }
		test_addresses.add("staff@threeriversvillageschool.org");

        ArrayList<String> from_addresses = new ArrayList<String>();
        from_addresses.add("office@threeriversvillageschool.org");
        from_addresses.add("evan@threeriversvillageschool.org");
        from_addresses.add("jmp@threeriversvillageschool.org");
        from_addresses.add("jancey@threeriversvillageschool.org");
        from_addresses.add("info@threeriversvillageschool.org");
        from_addresses.add("staff@threeriversvillageschool.org");

        e.parseMessage();
		return ok(views.html.view_pending_email.render(e, test_addresses, from_addresses));
	}

    public Result sendTestEmail() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        Email e = Email.findById(Integer.parseInt(values.get("id")[0]));
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

    public Result sendEmail() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        Email e = Email.findById(Integer.parseInt(values.get("id")[0]));
        e.parseMessage();

		int tagId = Integer.parseInt(values.get("tagId")[0]);
        Tag theTag = Tag.findById(tagId);
		String familyMode = values.get("familyMode")[0];
		Collection<Person> recipients = getTagMembers(tagId, familyMode);

		boolean hadErrors = false;

		for (Person p : recipients) {
			if (p.email != null && !p.email.equals("")) {
				try {
					MimeMessage to_send = new MimeMessage(e.parsedMessage);
					to_send.addRecipient(Message.RecipientType.TO,
								new InternetAddress(p.email, p.first_name + " " + p.last_name));
					to_send.setFrom(new InternetAddress(values.get("from")[0]));
					Transport.send(to_send);
				} catch (MessagingException ex) {
					ex.printStackTrace();
					hadErrors = true;
                } catch (UnsupportedEncodingException ex) {
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

    public Result deleteEmail() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        Email e = Email.findById(Integer.parseInt(values.get("id")[0]));
        e.delete();

        return ok();
    }

    public Result deletePerson(Integer id) {
        Person.delete(id);
        return redirect(routes.CRM.recentComments());
    }

    public Result editPerson(Integer id) {
        Person p = Person.findById(id);
        return ok(views.html.edit_person.render(p.fillForm()));
    }

    public Result savePersonEdits() {
        CachedPage.onPeopleChanged();

        Form<Person> personForm = Form.form(Person.class);
        Form<Person> filledForm = personForm.bindFromRequest();
        if(filledForm.hasErrors()) {
            System.out.println("ERRORS!");
            for (Map.Entry<String, List<ValidationError>> error : filledForm.errors().entrySet()) {
                System.out.println(error.getKey());
                for (ValidationError error2 : error.getValue()) {
                    System.out.println(error2.key() + ", " + error2.message());
                }
            }
            return badRequest(
                views.html.edit_person.render(filledForm)
            );
        }

        Person updatedPerson = Person.updateFromForm(filledForm);
        return redirect(routes.CRM.person(
                (updatedPerson.is_family ? updatedPerson.family_members.get(0) : updatedPerson).person_id));
    }

    public static String getInitials(Person p) {
        String result = "";
        if (p.first_name != null && p.first_name.length() > 0) {
            result += p.first_name.charAt(0);
        }
        if (p.last_name != null && p.last_name.length() > 0) {
            result += p.last_name.charAt(0);
        }
        return result;
    }

    public Result addComment() {
        CachedPage.remove(CachedPage.RECENT_COMMENTS);
        Form<Comment> filledForm = Form.form(Comment.class).bindFromRequest();
        Comment new_comment = new Comment();

        new_comment.person = Person.findById(Integer.parseInt(filledForm.field("person").value()));
        new_comment.user = mApplication.getCurrentUser();
        new_comment.message = filledForm.field("message").value();

        String task_id_string = filledForm.field("comment_task_ids").value();

        if (task_id_string.length() > 0 || new_comment.message.length() > 0) {
            new_comment.save();

            String[] task_ids = task_id_string.split(",");
            for (String id_string : task_ids) {
                if (!id_string.isEmpty()) {
                    int id = Integer.parseInt(id_string);
                    if (id >= 1) {
                        CompletedTask.create(Task.findById(id), new_comment);
                    }
                }
            }

            if (filledForm.field("send_email").value() != null) {
                for (NotificationRule rule :
                        NotificationRule.findByType(NotificationRule.TYPE_COMMENT)) {
                    play.libs.mailer.Email mail = new play.libs.mailer.Email();
                    mail.setSubject("DemSchoolTools comment: " + new_comment.user.name + " & " + getInitials(new_comment.person));
                    mail.addTo(rule.email);
                    mail.setFrom("DemSchoolTools <noreply@demschooltools.com>");
                    mail.setBodyHtml(views.html.comment_email.render(Comment.find.byId(new_comment.id)).toString());
                    play.libs.mailer.MailerPlugin.send(mail);
                }
            }

            return ok(views.html.comment_fragment.render(Comment.find.byId(new_comment.id), false));
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

    public static String formatDob(Date d) {
		if (d == null) {
			return "---";
		}
        return new SimpleDateFormat("MM/dd/yy").format(d);
    }

    public static String formatDate(Date d) {
        d = new Date(d.getTime() + OrgConfig.get().time_zone.getOffset(d.getTime()));
        Date now = new Date();

		long diffHours = (now.getTime() - d.getTime()) / 1000 / 60 / 60;

        // String format = "EEE MMMM d, h:mm a";
		String format;

		if (diffHours < 24) {
			format = "h:mm a";
		} else if (diffHours < 24 * 7) {
			format = "EEEE, MMMM d";
		} else {
            format = "MM/d/yy";
        }
        return new SimpleDateFormat(format).format(d);
    }

    public Result viewTaskList(Integer id) {
        TaskList list = TaskList.findById(id);
        List<Person> people = list.tag.people;

        return ok(views.html.task_list.render(list, people));
    }

    public Result viewMailchimpSettings() {
        MailChimpClient mailChimpClient = new MailChimpClient();
        Organization org = OrgConfig.get().org;

        Map<String, ListMethodResult.Data> mc_list_map =
            Public.getMailChimpLists(mailChimpClient, org.mailchimp_api_key);

        return ok(views.html.view_mailchimp_settings.render(
            Form.form(Organization.class), org,
            MailchimpSync.find.where().eq("tag.organization", OrgConfig.get().org).findList(),
            mc_list_map));
    }

    public Result saveMailchimpSettings() {
        final Map<String, String[]> values = request().body().asFormUrlEncoded();

        if (values.containsKey("mailchimp_api_key")) {
            OrgConfig.get().org.setMailChimpApiKey(values.get("mailchimp_api_key")[0]);
        }

        if (values.containsKey("mailchimp_updates_email")) {
            OrgConfig.get().org.setMailChimpUpdatesEmail(values.get("mailchimp_updates_email")[0]);
        }

        if (values.containsKey("sync_type")) {
            for (String tag_id : values.get("tag_id")) {
                Tag t = Tag.findById(Integer.parseInt(tag_id));
                MailchimpSync sync = MailchimpSync.create(t,
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
