const utils = require('./utils');
const Handlebars = require('handlebars');

let login_message_shown = false;

const insertIntoSortedList = function(charge, list, parent_el) {
    if (list.length === 0) {
        parent_el.append(charge.el);
        list.push(charge);
        return;
    }

    // Find first element in list that should appear after charge.
    let i = 0;
    for ( ; i<list.length; i++) {
        if (charge.name < list[i].name) {
            break;
        }
    }

    if (i == list.length) {
        list[i-1].el.after(charge.el);
        list.push(charge);
    } else {
        list[i].el.before(charge.el);
        list.splice(i, 0, charge);
    }
};

const indexOfCharge = function(list, charge) {
    for (let i = 0; i < list.length; i++) {
        if (list[i].id == charge.id) {
            return i;
        }
    }

    return -1;
};

const checkboxChanged = function(charge) {
    charge.checkbox.prop("disabled", true);
    let i = indexOfCharge(app.active_rps, charge);

    const url = "/setResolutionPlanComplete?id=" + charge.id +
        "&complete=" + (i >= 0);
    $.post(url)
        .done(function(data) {
            // We expect an empty response. If it is not empty, the user
            // probably got redirected to the login page.
            if (data !== "") {
                if (!login_message_shown) {
                    alert("Your change was not saved. You may not be logged in.");
                }
                login_message_shown = true;
                return;
            }
            charge.el.addClass("rp-moved");
            if (i >= 0) {
                charge.el.fadeOut("slow", function() {
                    charge.el.detach();
                    app.active_rps.splice(i, 1);
                    insertIntoSortedList(charge, app.completed_rps, $(".completed-rps"));
                    charge.el.show();
                });
            } else {
                i = indexOfCharge(app.completed_rps, charge);
                charge.el.fadeOut("slow", function() {
                    charge.el.detach();
                    app.completed_rps.splice(i, 1);
                    insertIntoSortedList(charge, app.active_rps, $(".active-rps"));
                    charge.el.show();
                });
            }
        })
        .fail(function() {
        })
        .always(function() {
            charge.checkbox.prop("disabled", false);
        });
};

const Charge = function(data, el) {
    const self = this;

    this.removeCharge = function() {
        self.el.remove();
        $.post("/removeCharge?id=" + self.id);
    };

    self.id = data.id;
    self.el = el;
    self.name = utils.displayName(data.person).toLowerCase();

    self.checkbox = el.find("input");
    self.checkbox.prop("checked", data.rpComplete);

    self.checkbox.change(function() {
        checkboxChanged(self);
    });
};

const addCharge = function(data, parent_el) {
    const template_data = {
        name: utils.displayName(data.person),
        caseNumber: data.theCase.caseNumber,
        closed_date: utils.reformatDate('m/dd', data.theCase.dateClosed),
        closed_day_of_week: utils.reformatDate('D', data.theCase.dateClosed),
        sm_date: utils.reformatDate('m/dd', data.smDecisionDate),
        sm_day_of_week: utils.reformatDate('D', data.smDecisionDate),
        rule_title: data.rule ? data.ruleTitle : config.show_entry ? "<No rule>" : "",
        resolutionPlan: data.resolutionPlan,
        smDecision: data.smDecision,
        findings: data.theCase.compositeFindings,
        smDecisionDate: data.smDecisionDate,
        referredToSm: data.referredToSm
        };

    if (data.dateClosed) {
        template_data.dateClosed = data.dateClosed;
    }

    const new_charge_el = parent_el.append(
        app.rp_template(template_data)).children(":last-child");

    return new Charge(data, new_charge_el);
};

const id_to_charge = {};
const id_to_case = {};

function loadCharge(charge_data, list, parent_el) {
    // If there are multiple charges in the same case, a charge will be
    // given as an ID number the second time it is listed. If this is an
    // ID number instead of an object, look it up in the map of charges
    // we have kept.
    if (typeof charge_data === 'number') {
        charge_data = id_to_charge[charge_data];
    }

    if (typeof charge_data.theCase === 'object') {
        id_to_case[charge_data.theCase.id] = charge_data.theCase;

        for (const i in charge_data.theCase.charges) {
            const c2_data = charge_data.theCase.charges[i];
            if (typeof c2_data === 'object') {
                id_to_charge[c2_data.id] = c2_data;
            }
        }
    } else {
        charge_data.theCase = id_to_case[charge_data.theCase];
    }

    insertIntoSortedList(
        addCharge(charge_data, parent_el),
        list,
        parent_el);
}

window.initRpList = function() {
    app.rp_template = Handlebars.compile($("#rp-template").html());
    for (var i in app.initial_data.active_rps) {
        loadCharge(app.initial_data.active_rps[i], app.active_rps, $(".active-rps"));
    }
    for (i in app.initial_data.completed_rps) {
        loadCharge(app.initial_data.completed_rps[i], app.completed_rps, $(".completed-rps"));
    }
    for (i in app.initial_data.nullified_rps) {
        loadCharge(app.initial_data.nullified_rps[i], app.nullified_rps, $(".nullified-rps"));
    }
}
