const Handlebars = require('handlebars');

const utils = require('./utils');

function dbTimeToUserTime(str) {
    if (str === null || str.length == 0) {
        return;
    }

    const splits = str.split(":");
    let hours = parseInt(splits[0]);
    const minutes = parseInt(splits[1]);

    let ampm = "AM";

    if (hours >= 12) {
        ampm = "PM";
    }

    if (hours > 12) {
        hours -= 12;
    }

    return "" + hours + ":" + utils.zeroPad(minutes) + " " + ampm;
}

function Day(data, start_input, end_input) {
    const self = this;

    self.activateCode = function() {
        const the_code = self.start_input.val();

        self.end_input.val("");
        self.end_input.hide();
        if (app.codes[the_code]) {
            self.color_bar.css("background-color", app.codes[the_code].color).show();
        } else {
            self.color_bar.hide();
        }

        self.code_mode = true;
    };

    self.deactivateCode = function() {
        self.code_mode = false;
        self.end_input.show();
        self.color_bar.hide();
    };

    self.checkForCode = function() {
        const the_code = self.start_input.val();

        if (the_code.length > 0 && !the_code.match(/[0-9]/)) {
            self.activateCode();
        } else {
            self.deactivateCode();
        }
    };

    self.onChange = function() {
        self.dirty = true;
        self.checkForCode();
    };

    self.onBlur = function() {
        self.start_input.val(utils.formatTime(self.start_input.val()));
        self.end_input.val(utils.formatTime(self.end_input.val()));
    };

    this.save = function() {
        self.dirty = false;
        let url = "/attendance/saveDay?day_id=" + self.id;
        if (self.code_mode) {
            url += "&code=" + self.start_input.val();
        } else {
            url += "&startTime=" + self.start_input.val() +
                "&endTime=" + self.end_input.val();
        }
        $.post(url);
    };

    self.start_input = $(start_input);
    self.end_input = $(end_input);
    self.color_bar = self.end_input.parent().find('.color-bar');
    self.id = data.id;
    self.dirty = false;
    self.code_mode = false;

    if (data.code) {
        self.start_input.val(data.code);
        self.checkForCode();
    } else {
        self.start_input.val(dbTimeToUserTime(data.startTime));
        self.end_input.val(dbTimeToUserTime(data.endTime));
    }

    self.start_input.blur(self.onBlur);
    self.end_input.blur(self.onBlur);
    self.start_input.on(utils.TEXT_AREA_EVENTS, self.onChange);
    self.end_input.on(utils.TEXT_AREA_EVENTS, self.onChange);

    return self;
}

function PersonRow(person, days, week, el) {
    const self = this;

    self.setDirty = function() { self.dirty = true; }

    this.removePerson = function() {
        $.post("/attendance/deletePersonWeek?personId=" + person.personId +
               "&monday=" + app.monday).done(function(data) {
            self.el.remove();
            app.person_rows.splice(app.person_rows.indexOf(self), 1);
            addAdditionalPerson(self.person);
        });
    };

    this.save = function() {
        self.dirty = false;
        $.post("/attendance/saveWeek?week_id=" + self.week.id +
               "&extraHours=" + self.week_el.val());
    };

    self.person = person;
    self.days = [];
    self.week = week;
    self.el = el;
    self.dirty = false;

    const inputs = el.find("input");
    self.el.find("img").click(self.removePerson);

    for (let i = 0; i < 5; i++) {
        self.days.push(new Day(days[i], inputs[i*2], inputs[i*2+1]));
    }

    self.week_el = $(inputs[10]);
    self.week_el.val(week.extraHours);

    self.week_el.on(utils.TEXT_AREA_EVENTS, self.setDirty);

    return self;
}

function addNewPersonRow(people) {
    const ids = [];
    for (let i = 0; i < people.length; i++) {
        ids.push(people[i].personId);
    }
    $.post("/attendance/createPersonWeek", {
            'personId[]': ids,
            monday: app.monday
        }).done(function(data) {
            const results = $.parseJSON(data);
            for (let i = 0; i < results.length; i++) {
                const result = results[i];
                loadRow(result.week.person, result.days, result.week, $(".table"));
            }
        });
}

function addAdditionalPerson(person) {
    const new_el = $("#additional-people").append(
        app.additional_person_template({
            name: person.firstName + " " + person.lastName
        })).children(":last-child");

    new_el.find("a").click(function () {
        addNewPersonRow([person]);
        new_el.remove();
    });
}

function addAllAdditionalPeople() {
    $("#additional-people").empty();
    addNewPersonRow(app.initial_data.additional_people);
}

function loadRow(person, days, week, dest_el) {
    let insert_before_i;
    for (let i = 0; i < app.person_rows.length; i++) {
        const p2 = app.person_rows[i].person;
        if ((p2.firstName + ' ' + p2.lastName) >
            (person.firstName + ' ' + person.lastName)) {
            insert_before_i = i;
            break;
        }
    }
    const new_row_el = $($.parseHTML(
        app.person_row_template({
            firstName: person.firstName,
            lastName: person.lastName,
        })));
    if (insert_before_i !== undefined) {
        app.person_rows[insert_before_i].el.before(new_row_el);
    } else {
        dest_el.append(new_row_el);
    }

    const new_row = new PersonRow(person, days, week, new_row_el);
    if (insert_before_i !== undefined) {
        app.person_rows.splice(insert_before_i, 0, new_row);
    } else {
        app.person_rows.push(new_row);
    }
}

function setNoSchool(day_num) {
    for (const i in app.person_rows) {
        app.person_rows[i].days[day_num].start_input.val("_NS_");
        app.person_rows[i].days[day_num].onChange();
    }
}

function handleNoSchoolButton(day_num) {
    return function() {
        $( "#dialog-confirm" ).dialog({
              resizable: false,
              height: 240,
              modal: true,
              buttons: {
                "Erase existing data": function() {
                    setNoSchool(day_num);
                    $( this ).dialog( "close" );
                },
                Cancel: function() {
                     $( this ).dialog( "close" );
                }
              }
            });
    };
}

function saveIfNeeded() {
    for (const i in app.person_rows) {
        if (app.person_rows[i].dirty) {
            app.person_rows[i].save();
        }
        for (const j in app.person_rows[i].days) {
            if (app.person_rows[i].days[j].dirty) {
                app.person_rows[i].days[j].save();
            }
        }
    }

    window.setTimeout(saveIfNeeded, 2000);
}

window.initAttendanceWeek = function() {
    app.person_row_template = Handlebars.compile($("#person-row-template").html().trim());
    app.additional_person_template =
        Handlebars.compile($("#additional-person-template").html());

    const no_school_buttons = $("button.no-school");
    for (var i = 0; i < 5; i++) {
        const button = no_school_buttons[i];
        $(button).click(handleNoSchoolButton(i));
    }

    for (i in app.initial_data.active_people) {
        var person = app.initial_data.active_people[i];
        loadRow(person,
                app.initial_data.days[person.personId],
                app.initial_data.weeks[person.personId],
                $('.attendance-view tbody'));
    }

    for (i in app.initial_data.additional_people) {
        person = app.initial_data.additional_people[i];
        addAdditionalPerson(person);
    }

    saveIfNeeded();

    $("button.add-all").click(addAllAdditionalPeople);
};
