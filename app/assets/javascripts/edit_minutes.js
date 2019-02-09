var Handlebars = require('handlebars');

var utils = require('./utils');
var chooser = require('./chooser');
var people_chooser = require('./people_chooser');

const time_served_string = "Time served";

const SAVE_TIMEOUT = 2000;

function showSomethingInSidebar(url) {
    $("#sidebar").html("<h2>Loading...</h2>");
    $.get(url,
          null,
           function(data, status, jqXHR) {
            // Remove a href links from data to avoid inadvertent clicks
            var patt = /<a nosidebar href="[^"]+"/gi;

            data = data.replace(patt, "<a ");

            $("#sidebar").html(data);
            $("#sidebar").find("table.sortable").each(function (i) {
                sorttable.makeSortable(this);
                sorttable.innerSortFunction.apply($(this).find("th")[1], []);
            });
            utils.limitHeight('.should-limit');
    });
}

function showPersonHistoryInSidebar(person) {
    showSomethingInSidebar("/personHistory/" + person.id);
}

function showRuleHistoryInSidebar(rule_id) {
    showSomethingInSidebar("/ruleHistory/" + rule_id);
}

function Charge(charge_id, el) {
    var self = this;

    var SEVERITIES = {
        "mild": "Mild",
        "moderate": "Moderate",
        "serious": "Serious",
        "severe": "Severe"
    };

    this.checkReferralLabelHighlight = function() {
        if (el.find(".minor-referral-destination").val()) {
            el.find(".minor-referral-label").addClass("highlight");
        } else {
            el.find(".minor-referral-label").removeClass("highlight");
        }
    };

    this.loadData = function(json, referenced_charge) {
        el.find(".resolution_plan").val(json.resolution_plan);

        if (json.plea == "Guilty") {
            el.find(".plea-guilty").prop("checked", true);
        } else if (json.plea == "No Contest") {
            el.find(".plea-no-contest").prop("checked", true);
        } else if (json.plea == "Not Guilty") {
            el.find(".plea-not-guilty").prop("checked", true);
        } else if (json.plea == "N/A") {
            el.find(".plea-na").prop("checked", true);
        }

        // rule, person
        if (json.person) {
            self.people_chooser.addPerson(
                json.person.person_id,
                utils.displayName(json.person));
        }

        if (json.rule) {
            self.rule_chooser.loadData(json.rule);
        }

        if (json.referred_to_sm) {
            el.find(".refer-to-sm").prop("checked", true);
        }

        for (var key in SEVERITIES) {
            if (json.severity == SEVERITIES[key]) {
                el.find(".severity-" + key).prop("checked", true);
            }
        }

        el.find(".minor-referral-destination").val(json.minor_referral_destination);
        self.checkReferralLabelHighlight();

        if (referenced_charge) {
            self.referenced_charge = referenced_charge;
            if (referenced_charge.has_default_rule) {
                el.addClass("breaking-res-plan");
            }
            el.find(".rp-followups").show();
            el.find(".original-res-plan").show();
            el.find(".original-res-plan-text").html(referenced_charge.resolution_plan);

            if (json.resolution_plan === time_served_string) {
                el.find(".rp-followup-time-served").prop("checked", true);
                el.find(".rp-row").hide();
            } else if (json.resolution_plan) {
                el.find(".rp-followup-new-rp").prop("checked", true);
            } else {
                el.find(".rp-row").hide();
            }
        }

        if (json.is_referenced) {
            el.find(".remove-charge").addClass("disabled");
            el.find(".remove-charge-disabled-text").show();
        }
    };

    this.saveIfNeeded = function() {
        window.setTimeout(self.saveIfNeeded, 5000);
        if (!self.is_modified) {
            return;
        }

        var url = "/saveCharge?id=" + charge_id;

        if (self.people_chooser.people.length > 0) {
            url += "&person_id=" + self.people_chooser.people[0].id;
        }

        var resolution_plan = el.find(".resolution_plan").val();
        if (el.find(".rp-followups").is(":visible") && el.find(".rp-followup-time-served").prop("checked")) {
            resolution_plan = time_served_string;
        }
        url += "&resolution_plan=" + encodeURIComponent(resolution_plan);

        for (var key in SEVERITIES) {
            if (el.find(".severity-" + key).prop("checked")) {
                url += "&severity=" + SEVERITIES[key];
            }
        }

        var plea = el.find(".plea-guilty");
        if (plea.prop("checked")) {
            url += "&plea=Guilty";
        }

        plea = el.find(".plea-no-contest");
        if (plea.prop("checked")) {
            url += "&plea=No Contest";
        }

        plea = el.find(".plea-not-guilty");
        if (plea.prop("checked")) {
            url += "&plea=Not Guilty";
        }

        plea = el.find(".plea-na");
        if (plea.prop("checked")) {
            url += "&plea=N/A";
        }

        var refer = el.find(".refer-to-sm");
        url += "&referred_to_sm=" + refer.prop("checked");

        var minor_referral_destination = el.find(".minor-referral-destination");
        if (minor_referral_destination.length > 0) {
            url += "&minor_referral_destination=" +
                encodeURIComponent(minor_referral_destination.val());
        }

        if (self.rule_chooser.results[0]) {
            url += "&rule_id=" + self.rule_chooser.results[0];
        }

        $.post(url, function(data) {
            self.is_modified = false;
        });
    };

    this.removeCharge = function() {
        if (self.referenced_charge) {
            self.el.parents('.case').find('.case-charge-reference-generate[data-id=' + self.referenced_charge.id + ']')
                .show().parent().find('.case-charge-reference-already-generated').hide();
        }
        $.post("/removeCharge?id=" + charge_id, function() {
            self.el.remove();
            // Don't try to save any remaining modifications if
            // this charge has been removed.
            self.is_modified = false;
            // TODO: remove from Case.charges list
        });
    };

    this.markAsModified = function() {
        self.is_modified = true;
        if ((self.people_chooser.people.length === 0) ||
            !self.rule_chooser.results[0]) {
            el.find(".last-rp").html("");
        }

        if (self.people_chooser.people.length == 1 && self.rule_chooser.results[0]) {
            var url = "/getLastRp";
            url += "/" + self.people_chooser.people[0].id;
            url += "/" + self.rule_chooser.results[0];
            $.get(url, function (data) {
                el.find(".last-rp").html(data);
                el.find(".last-rp .more-info").click(function() {
                    showSomethingInSidebar(
                        '/personRuleHistory' +
                        '/' + self.people_chooser.people[0].id +
                        '/' + self.rule_chooser.results[0]);
                });
            });
        }

        self.checkReferralLabelHighlight();
    };

    this.old_rp = null;
    this.checkText = function() {
        if (el.find(".resolution_plan").val() !== self.old_rp) {
            self.markAsModified();
            self.old_rp = el.find(".resolution_plan").val();
        }
    };

    self.el = el;
    self.is_modified = false;
    window.setTimeout(self.saveIfNeeded, 5000);

    self.remove_button = el.find("button");
    self.remove_button.click(self.removeCharge);

    el.find(".resolution_plan").change(self.markAsModified);
    el.find(".resolution_plan").on(utils.TEXT_AREA_EVENTS, self.checkText);
    el.find(".plea-guilty").change(self.markAsModified);
    el.find(".plea-no-contest").change(self.markAsModified);
    el.find(".plea-na").change(self.markAsModified);
    el.find(".plea-not-guilty").change(self.markAsModified);
    el.find(".plea-not-guilty").change(function() {
            self.el.find(".refer-to-sm").prop("checked", true);
    });
    el.find(".refer-to-sm").change(self.markAsModified);
    el.find(".minor-referral-destination").change(self.markAsModified);
    el.find(".minor-referral-destination").on(utils.TEXT_AREA_EVENTS, self.markAsModified);

    el.find(".rp-followup").change(function() {
        if ($(this).hasClass("rp-followup-new-rp")) {
            el.find(".rp-row").show();
        } else {
            el.find(".rp-row").hide();
            el.find(".resolution_plan").val("");
        }
        self.markAsModified();
    });

    self.people_chooser = new people_chooser.PeopleChooser(
        el.find(".people_chooser"), self.markAsModified, self.markAsModified,
        app.people, showPersonHistoryInSidebar);
    self.people_chooser.setOnePersonMode(true);

    function getLabel(json) {
        return json.title;
    }

    self.rule_chooser = new chooser.Chooser(
        el.find(".rule_chooser"),
        false,
        2,
        app.rules,
        getLabel,
        showRuleHistoryInSidebar,
        self.markAsModified);

    el.find(".plea[type=radio]").prop("name", "plea-" + charge_id);

    // el.mouseleave(function() { self.remove_button.hide(); } );
    // el.mouseenter(function() { self.remove_button.show(); } );
    for (var key in SEVERITIES) {
        el.find(".severity-" + key).change(self.markAsModified);
    }
    el.find(".severity[type=radio]").prop("name", "severity-" + charge_id);

    el.find(".rp-followup")
        .prop("name", "rp-followup-" + charge_id)
        .change(self.markAsModified);
}

function Case (id, el) {
    var self = this;

    this.saveIfNeeded = function() {
        window.setTimeout(self.saveIfNeeded, SAVE_TIMEOUT);
        if (!self.is_modified) {
            return;
        }

        $.post("/saveCase?id=" + id, {
            closed: !self.el.find("input.continued").prop("checked"),
            date: self.el.find(".date").val(),
            findings: self.el.find(".findings").val(),
            location: self.el.find(".location").val(),
            time: self.el.find(".time").val()
        }, function(data) {
            self.is_modified = false;
        });
    };

    this.markAsModified = function() {
        self.is_modified = true;
    };

    this.loadData = function(data) {
        el.find(".location").val(data.location);
        el.find(".date").val(data.date);
        el.find(".time").val(data.time);
        el.find(".findings").val(data.findings);
        el.find("input.continued").prop("checked", data.date_closed === null);

        for (var i in data.people_at_case) {
            var pac = data.people_at_case[i];
            if (pac.role == app.ROLE_TESTIFIER) {
                self.testifier_chooser.addPerson(
                    pac.person.person_id,
                    utils.displayName(pac.person));
            } else if (pac.role == app.ROLE_WRITER) {
                self.writer_chooser.addPerson(
                    pac.person.person_id,
                    utils.displayName(pac.person));
            }
        }

        $.get("/getCaseReferencesJson?case_id=" + self.id, function(case_references) {
            setCaseReferences(case_references);
            var case_references_json = $.parseJSON(case_references);
            for (i in data.charges) {
                var ch = data.charges[i];
                var new_charge = self.addChargeNoServer(ch.id);
                new_charge.loadData(ch, findReferencedCharge(ch.id, case_references_json));
            }
        });

        function findReferencedCharge(charge_id, case_references) {
            for (var i in case_references) {
                var c = case_references[i];
                for (var j in c.charges) {
                    var ch = c.charges[j];
                    if (ch.generated_charge_id == charge_id) {
                        return {
                            id: ch.charge_id,
                            resolution_plan: ch.resolution_plan,
                            has_default_rule: ch.has_default_rule
                        };
                    }
                }
            }
            return null;
        }
    };

    this.addCharge = function() {
        $('body').animate({'scrollTop': $('body').scrollTop() + 100}, 'slow');
        $.post("/addCharge?case_id=" + id,
               function(data, textStatus, jqXHR) {
            self.addChargeNoServer(parseInt(data));
        } );
    };

    this.addChargeNoServer = function(charge_id) {
        var new_charge_el = self.el.find(".charges").append(
            app.charge_template()).children(":last-child");
        var new_charge = new Charge(charge_id, new_charge_el);
        self.charges.push(new_charge);
        return new_charge;
    };

    this.checkText = function() {
        if (el.find(".findings").val() !== self.old_findings) {
            self.markAsModified();
            self.old_findings = el.find(".findings").val();
        }
    };

    this.clearCase = function() {
        $( "#dialog-confirm" ).dialog({
              resizable: false,
              height: 240,
              modal: true,
              buttons: {
                "Erase case": function() {
                    self.clearCaseNoConfirm();
                    $( this ).dialog( "close" );
                },
                "Cancel": function() {
                     $( this ).dialog( "close" );
                }
              }
            });
     }

     this.clearCaseNoConfirm = function() {
        el.find(".location").val('');
        el.find(".date").val('');
        el.find(".time").val('');
        el.find(".findings").val('');
        el.find("input.continued").prop("checked", false);

        if (self.testifier_chooser) {
          self.testifier_chooser.clear();
        }
        if (self.writer_chooser) {
          self.writer_chooser.clear();
        }
        if (self.case_chooser) {
          self.case_chooser.clear();
        }

        var url = "/clearAllReferencedCases?case_id=" + self.id;
        $.post(url, setCaseReferences);

        for (var i = 0; i < self.charges.length; i++) {
            self.charges[i].removeCharge();
        }
        self.charges = [];
        self.markAsModified();
    };

    this.id = id;
    this.el = el;
    this.old_findings = null;

    if (config.track_writer) { // eslint-disable-line
        this.writer_chooser = new people_chooser.PeopleChooser(el.find(".writer"),
            function(person) {
                $.post("/addPersonAtCase?case_id=" + id +
                       "&person_id=" + person.id +
                       "&role=" + app.ROLE_WRITER);
            },
            function(person) {
                $.post("/removePersonAtCase?case_id=" + id +
                       "&person_id=" + person.id +
                       "&role=" + app.ROLE_WRITER);
            },
            app.people, showPersonHistoryInSidebar);
    }

    this.testifier_chooser = new people_chooser.PeopleChooser(
        el.find(".testifier"),
        function(person) {
            $.post("/addPersonAtCase?case_id=" + id +
                   "&person_id=" + person.id +
                   "&role=" + app.ROLE_TESTIFIER);
        },
        function(person) {
            $.post("/removePersonAtCase?case_id=" + id +
                   "&person_id=" + person.id +
                   "&role=" + app.ROLE_TESTIFIER);
        },
        app.people, showPersonHistoryInSidebar);
    this.is_modified = false;
    this.charges = [];

    function setCaseReferences(data) {
        var json = $.parseJSON(data);
        self.case_chooser.loadData(json);
        var el = self.el.find('.case-references');
        el.html('');
        for (var i in json) {
            el.append(app.case_reference_template(json[i]));
        }
        el.find('input').click(function() {
            var parent = $(this).parents('.case-charge-reference');
            if (this.checked) {
                $.post("/addChargeReferenceToCase?case_id=" + self.id + "&charge_id=" + $(this).data('id'));
                parent.addClass('referenced');
            } else {
                $.post("/removeChargeReferenceFromCase?case_id=" + self.id + "&charge_id=" + $(this).data('id'));
                parent.removeClass('referenced');
            }
        });
        el.find('.case-charge-reference-generate').click(function() {
            $(this).hide().parent().find('.case-charge-reference-already-generated').show();
            var referenced_charge = {
                id: $(this).data('id'),
                resolution_plan: $(this).data('resolution-plan')
            };
            var url = "/generateChargeFromReference?case_id=" + id + "&referenced_charge_id=" + $(this).data('id');
            $.post(url, function(response) {
                var new_charge_json = $.parseJSON(response);
                if (new_charge_json.rule) {
                    referenced_charge.has_default_rule = true;
                }
                var new_charge = self.addChargeNoServer(new_charge_json.id);
                new_charge.loadData(new_charge_json, referenced_charge);
            });
        });
    }

    function getLabel(json) {
        return json.case_number;
    }

    self.case_chooser = new chooser.Chooser(el.find(".case_chooser"), true, 5, app.cases, getLabel, null, null,
        function(id) {
            var url = "/addReferencedCase?case_id=" + self.id + "&referenced_case_id=" + id;
            $.post(url, setCaseReferences);
        },
        function(id) {
            var url = "/removeReferencedCase?case_id=" + self.id + "&referenced_case_id=" + id;
            $.post(url, setCaseReferences);
        });

    el.find(".location").change(self.markAsModified);
    el.find(".findings").change(self.markAsModified);
    el.find(".findings").on(utils.TEXT_AREA_EVENTS, self.checkText);
    el.find(".date").change(self.markAsModified);
    el.find(".time").change(self.markAsModified);
    el.find("input.continued").change(self.markAsModified);

    el.find(".add-charges").click(self.addCharge);
    el.find(".clear-case").click(self.clearCase);

    window.setTimeout(self.saveIfNeeded, SAVE_TIMEOUT);
}

function addPersonAtMeeting(person, role) {
    $.post("/addPersonAtMeeting?meeting_id=" + app.meeting_id +
           "&person_id=" + person.id +
           "&role=" + role );
}

function removePersonAtMeeting(person, role) {
    $.post("/removePersonAtMeeting?meeting_id=" + app.meeting_id +
           "&person_id=" + person.id +
           "&role=" + role );
}

function makePeopleChooser(selector, role) {
    return new people_chooser.PeopleChooser(
        $(selector),
        function(person) { addPersonAtMeeting(person, role); },
        function(person) { removePersonAtMeeting(person, role); },
        app.people, showPersonHistoryInSidebar);
}

function loadInitialData() {
    app.committee_chooser.loadPeople(app.initial_data.committee);
    app.chair_chooser.loadPeople(app.initial_data.chair);
    app.notetaker_chooser.loadPeople(app.initial_data.notetaker);
    app.sub_chooser.loadPeople(app.initial_data.sub);
    app.runner_chooser.loadPeople(app.initial_data.runners);

    for (var i in app.initial_data.cases) {
        var data = app.initial_data.cases[i];
        var new_case = addCaseNoServer(data.id, data.case_number);
        new_case.loadData(data);
    }
}

function checkDirties() {
    var num_dirty = 0;

    for (var i in app.cases) {
        var c = app.cases[i];
        if (c.is_modified) {
            num_dirty++;
        }
        for (var j in c.charges) {
            var ch = c.charges[j];
            if (ch.is_modified) {
                num_dirty++;
            }
        }
    }

    if (num_dirty < 3) {
        $("#notice").hide();
    } else {
        $("#notice").show();
        $("#notice").html("" + num_dirty + " unsaved changes. Connection lost?");
    }

    window.setTimeout(checkDirties, 1000);
}

function continueCase(event) {
    var list_item = $(event.target).parent();
    var id = list_item.prop("id");
    var splits = id.split("-");
    var case_id = splits[splits.length-1];

    $.post("/continueCase?meeting_id=" + app.meeting_id +
           "&case_id=" + case_id, "",
           function(data, textStatus, jqXHR) {
        var case_data = $.parseJSON(data);
        var new_case = addCaseNoServer(case_data.id, case_data.case_number);
        new_case.loadData(case_data);
        $('body').animate({'scrollTop': new_case.el.offset().top + 500}, 'slow');
        list_item.remove();
    });
}

window.initMinutesPage = function() {
    Handlebars.registerHelper('render', function(partialId, options) {
      var selector = 'script[type="text/x-handlebars-template"]#' + partialId;
      var source = $(selector).html();
      var html = Handlebars.compile(source)(options.hash);

      return new Handlebars.SafeString(html);
    });

    $("#meeting").append(Handlebars.compile($("#meeting-template").html())());

    Handlebars.registerPartial("people-chooser", $("#people-chooser").html());
    Handlebars.registerPartial("rule-chooser", $("#rule-chooser").html());
    Handlebars.registerPartial("case-chooser", $("#case-chooser").html());

    app.case_template = Handlebars.compile($("#case-template").html());
    app.charge_template = Handlebars.compile($("#charge-template").html());
    app.case_reference_template = Handlebars.compile($("#case-reference-template").html());

    app.committee_chooser =
        makePeopleChooser(".committee", app.ROLE_JC_MEMBER);

    app.chair_chooser =
        makePeopleChooser(".chair", app.ROLE_JC_CHAIR);
    app.chair_chooser.setOnePersonMode(true);

    app.notetaker_chooser =
        makePeopleChooser(".notetaker", app.ROLE_NOTE_TAKER);
    app.sub_chooser =
        makePeopleChooser(".sub", app.ROLE_JC_SUB);
    app.runner_chooser =
        makePeopleChooser(".runner", app.ROLE_RUNNER);

    loadInitialData();

    $(".continue-cases li").click(continueCase);
    $("button.add-case").click(addCase);

    window.onbeforeunload = function(e) {
        var dirty = false;
        for (var i in app.cases) {
            var c = app.cases[i];
            if (c.is_modified) {
                dirty = true;
            }
            for (var j in c.charges) {
                var ch = c.charges[j];
                if (ch.is_modified) {
                    dirty = true;
                }
            }
        }

        if (dirty) {
            return "Unsaved changes! Please give me a few more seconds...";
        }
        return null;
    };

    checkDirties();
};

function addCaseNoServer(id, number) {
    var new_case = $("#meeting-cases")
        .append(app.case_template({"num": number}))
        .children(":last-child");

    var case_obj = new Case(id, new_case);
    app.cases.push(case_obj);

    $("#meeting-cases").append(case_obj.el);
    case_obj.el.find(".date").datepicker({
        showOtherMonths: true,
        selectOtherMonths: true});
    case_obj.el.find(".date").datepicker("option", "dateFormat", "yy-mm-dd");

    return case_obj;
}

function addCase() {
    $.post("/newCase?meeting_id=" + app.meeting_id, "",
           function(data, textStatus, jqXHR) {
        var id_num_pair = $.parseJSON(data);
        var new_case = addCaseNoServer(id_num_pair[0], id_num_pair[1]);
        $('body').animate({'scrollTop': new_case.el.offset().top + 500}, 'slow');
    });
}
