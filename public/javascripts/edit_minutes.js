next_case_num = 1;

function Person(id, name) {
    this.name = name;
    this.id = id;

    this.render = function() {
        return app.person_template({"name": name});
    }
}

function PeopleChooser(el, on_add, on_remove) {
    this.el = el;
    this.people = [];
    var self = this;

    this.search_box = el.find(".person_search");
    this.search_box.autocomplete({
        source: "/jsonPeople",
    });

    this.search_box.bind( "autocompleteselect", function(event, ui) {
        new_person = self.addPerson(ui.item.id, ui.item.label);

        if (on_add) {
            on_add(new_person);
        }

        self.search_box.val('');
        event.preventDefault(); // keep jquery from inserting name into textbox
    });

    this.addPerson = function(id, name) {
        p = new Person(id, name);
        self.people.push(p);
        el.prepend(p.render());

        return p;
    }

    this.loadPeople = function(people) {
        for (i in people) {
            self.addPerson(people[i].id, people[i].name);
        }
    }
}

function Case (id, el) {
    var self = this;

    this.saveIfNeeded = function() {
        window.setTimeout(self.saveIfNeeded, 2000);
        if (!self.is_modified) {
            return;
        }

        url = "/saveCase?id=" + id;
        if (self.writer_chooser.people.length > 0) {
            url += "&writer_id=" + self.writer_chooser.people[0].id;
        }
        url += "&location=" + encodeURIComponent(self.el.find(".location").val());
        url += "&findings=" + encodeURIComponent(self.el.find(".findings").val());
        url += "&date=" + encodeURIComponent(self.el.find(".date").val());

        self.is_modified = false;
        $.post(url);
    }

    this.markAsModified = function() {
        self.is_modified = true;
    }

    this.loadData = function(data) {
        el.find(".location").val(data.location);
        el.find(".date").val(data.date);
        el.find(".findings").val(data.findings);

        if (data.writer) {
            self.writer_chooser.addPerson(data.writer.person_id,
                  data.writer.first_name + " " + data.writer.last_name);
        }
    }

    this.id = id
    this.el = el;
    this.writer_chooser = new PeopleChooser(el.find(".writer"), self.markAsModified);
    this.testifier_chooser = new PeopleChooser(el.find(".testifier"));
    this.is_modified = false;

    el.find(".location").change(self.markAsModified);
    el.find(".findings").change(self.markAsModified);
    el.find(".date").change(self.markAsModified);

    window.setTimeout(self.saveIfNeeded, 2000);
}

function addPersonAtMeeting(person, role) {
    $.post("/addPersonAtMeeting?meeting_id=" + app.meeting_id
           + "&person_id=" + person.id +
           "&role=" + role );
}

function loadInitialData() {
    app.committee_chooser.loadPeople(app.initial_data.committee);
    app.chair_chooser.loadPeople(app.initial_data.chair);
    app.notetaker_chooser.loadPeople(app.initial_data.notetaker);

    for (i in app.initial_data.cases) {
        data = app.initial_data.cases[i];
        new_case = addCaseNoServer(data.case_number);
        new_case.loadData(data);
    }
}

$(function () {
    Handlebars.registerPartial("people-chooser", $("#people-chooser").html());

    app.case_template = Handlebars.compile($("#case-template").html());
    app.person_template = Handlebars.compile($("#person-template").html());
    app.testimony_template = Handlebars.compile($("#testimony-template").html());

    app.committee_chooser = new PeopleChooser($(".committee"),
         function(person) { addPersonAtMeeting(person, app.ROLE_JC_MEMBER) });

    app.chair_chooser = new PeopleChooser($(".chair"),
         function(person) { addPersonAtMeeting(person, app.ROLE_JC_CHAIR) });

    app.notetaker_chooser = new PeopleChooser($(".notetaker"),
        function(person) { addPersonAtMeeting(person, app.ROLE_NOTE_TAKER) });

    loadInitialData();
});

function addCaseNoServer(id)
{
    new_case = $("#meeting-cases").append(
        app.case_template({"num": id})).
        children(":last-child");

    var case_obj = new Case(id, new_case);
    app.cases.push(case_obj);

    $("#meeting-cases").append(case_obj.el);

    next_case_num += 1;

    return case_obj;
}

function addCase()
{
    d = new Date();

    case_id = (d.getMonth() + 1) + "-" + (d.getDate()) + "-";
    if (next_case_num < 10) {
        case_id += "0";
    }
    case_id += next_case_num;
    $.post("/newCase?id=" + case_id +
           "&meeting_id=" + app.meeting_id, "",
           function(data, textStatus, jqXHR) {
        new_case = addCaseNoServer(case_id);
        $('body').animate({'scrollTop': new_case.el.offset().top + 500}, 'slow');
    });
}

