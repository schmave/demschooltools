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
        new_person = new Person(ui.item.id, ui.item.label);
        self.people.push(new_person);

        if (on_add) {
            on_add(new_person);
        }

        el.append(new_person.render());
        self.search_box.val('');
        event.preventDefault(); // keep jquery from inserting name into textbox
    });
}

function Case (el) {
    this.el = el;
    this.writer_chooser = new PeopleChooser(el.find(".writer"));
    this.testifier_chooser = new PeopleChooser(el.find(".testifier"));
    var self = this;

    this.add_testimony = function() {
        self.el.find(".testimony").append(app.testimony_template());
    }

    el.find(".add_testimony").click(this.add_testimony);
}

function addPersonAtMeeting(person, role) {
    $.post("/addPersonAtMeeting?meeting_id=" + app.meeting_id
           + "&person_id=" + person.id +
           "&role=" + role );
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
});

function addCase()
{
	new_case = $("#meeting").append(
        app.case_template({"num": "09-30-" + next_case_num})).
        children(":last-child");

    var case_obj = new Case(new_case);

    $("#meeting").append(case_obj.el);

    next_case_num += 1;
}

