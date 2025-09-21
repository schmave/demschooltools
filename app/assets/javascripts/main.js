require('bootstrap');
require('jquery-form');
require('jquery-ui/ui/widgets/autocomplete');
require('jquery-ui/ui/widgets/datepicker');
require('jquery-ui/ui/widgets/dialog');
require('spectrum-colorpicker');

const utils = require('./utils');
require('./edit_attendance_week');
require('./attendance_person');
require('./edit_minutes');
require('./edit_rp_list');
const feedback_modal = require('./feedback_modal');
require('./sorttable');
const people_chooser = require('./people_chooser');
const chooser = require('./chooser');
const create_transaction = require('./create_transaction');
const roles = require('./roles');
const transaction_list = require('./transaction_list');
const settings_page = require('./settings_page');
const off_campus = require('./off_campus');
const attendance_rule = require('./attendance_rule');

$(function () {
    // Fix for bootstrap tabs not remembering their active tab
    // when you come back with the back button.
    // Thanks to this github gist:
    //   https://gist.github.com/josheinstein/5586469
    if (location.hash.substr(0, 2) == '#!') {
        $("a[href='#" + location.hash.substr(2) + "']").tab('show');
    }

    $('[data-toggle="tooltip"]').tooltip();

    $("a[data-toggle='tab']").on('shown.bs.tab', function (e) {
        const hash = $(e.target).attr('href');
        if (hash.substr(0, 1) == '#') {
            location.replace('#!' + hash.substr(1));
        }
    });

    $('#same_family_name').autocomplete({
        source: '/jsonPeople',
        minLength: 2,
    });

    $('#same_family_name').bind('autocompleteselect', function (event, ui) {
        $('#same_family_id').val(ui.item.id);
    });

    $('#navbar_people_search').autocomplete({
        minLength: 2,
        select: function (event, ui) {
            window.location.href = '/people/' + ui.item.id;
        },
        source: '/jsonPeople',
    });

    $('#navbar_attendance_people_search').autocomplete({
        minLength: 2,
        select: function (event, ui) {
            window.location.href = '/attendance/forPerson/' + ui.item.id;
        },
        source: '/attendance/jsonPeople',
    });

    $('.task_checkbox').click(function (event) {
        $('#new_comment').show();
        $('#comment_tasks').empty();
        $('#comment_task_ids').empty();

        const checked_boxes = $('.task_checkbox');
        for (let i = 0; i < checked_boxes.length; i++) {
            const box = checked_boxes[i];
            const id = box.id.split('_')[2];
            if (!box.disabled && box.checked) {
                $('#comment_tasks').append(
                    "<span class='label label-info'>" +
                        $('label[for=' + box.id + ']').text() +
                        '</span><br>',
                );
                $('#comment_task_ids').append(',' + id);
            }
        }
    });

    $('input.date').datepicker({
        showOtherMonths: true,
        selectOtherMonths: true,
        changeMonth: true,
        changeYear: true,
        dateFormat: 'yy-mm-dd',
    });

    utils.limitHeight('.should-limit');

    feedback_modal.init();

    $(function () {
        $('input.date.attendance-week').datepicker(
            'option',
            'onSelect',
            function (str_date) {
                location.href = '/attendance/viewWeek?date=' + str_date;
            },
        );
    });
});

window.initPeopleChooser = function (selector, onAdd, onRemove) {
    return new people_chooser.PeopleChooser(
        $(selector),
        onAdd,
        onRemove,
        '/jsonPeople',
    );
};

window.initChooser = function (
    el,
    allowMultiple,
    minLength,
    source,
    getLabel,
    onClick,
    onChange,
    onAdd,
    onRemove,
    initialData,
) {
    const myChooser = new chooser.Chooser(
        el,
        allowMultiple,
        minLength,
        source,
        getLabel,
        onClick,
        onChange,
        onAdd,
        onRemove,
    );
    myChooser.loadData(initialData);
    return myChooser;
};

window.enableButtonForCheckboxes = function (btn_selector, checkbox_class) {
    const checkbox_selector = 'input[type=checkbox].' + checkbox_class;
    $(checkbox_selector).change(function () {
        const count = $(checkbox_selector + ':checked').length;
        $(btn_selector).prop('disabled', count == 0);
    });
};

window.initCreateTransaction = function (accounts) {
    return create_transaction.init(accounts);
};

window.initTransactionList = function () {
    return transaction_list.init();
};

window.initRoles = function (rolesJson, people, terms, canEdit) {
    return roles.init(rolesJson, people, terms, canEdit);
};

window.initRolesRecordsReport = function (people) {
    return roles.initRecordsReport(people);
};

window.initSettingsPage = function () {
    return settings_page.init();
};

window.initOffCampus = function (people) {
    return off_campus.init(people);
};

window.initAttendanceRule = function (
    selectedPersonId,
    selectedPersonName,
    people,
) {
    return attendance_rule.init(selectedPersonId, selectedPersonName, people);
};
