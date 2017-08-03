require('bootstrap');
require('jquery-form');
require('jquery-ui/ui/widgets/autocomplete');
require('jquery-ui/ui/widgets/datepicker');
require('jquery-ui/ui/widgets/dialog');
require('spectrum-colorpicker');

var utils = require('./utils');
require('./edit_attendance_week');
require('./edit_entry');
require('./edit_minutes');
require('./edit_rp_list');
var feedback_modal = require('./feedback_modal');
require('./sorttable');

$(function() {
    // Fix for bootstrap tabs not remembering their active tab
    // when you come back with the back button.
    // Thanks to this github gist:
    //   https://gist.github.com/josheinstein/5586469
    if (location.hash.substr(0,2) == "#!") {
        $("a[href='#" + location.hash.substr(2) + "']").tab("show");
    }

    $("a[data-toggle='tab']").on("shown.bs.tab", function (e) {
        var hash = $(e.target).attr("href");
        if (hash.substr(0,1) == "#") {
            location.replace("#!" + hash.substr(1));
        }
    });

    $( "#same_family_name" ).autocomplete({
        source: "/jsonPeople",
        minLength: 2
    });

    $( "#same_family_name" ).bind( "autocompleteselect", function(event, ui) {
        $("#same_family_id").val(ui.item.id);
    });

    $( "#navbar_people_search" ).autocomplete({
        source: "/jsonPeople",
        minLength: 2
    });

    $( "#navbar_people_search" ).bind( "autocompleteselect", function(event, ui) {
        window.location.href="/people/" + ui.item.id;
    });

    $( ".task_checkbox" ).click( function (event) {
        $('#new_comment').show();
        $("#comment_tasks").empty();
        $("#comment_task_ids").empty();

        checked_boxes = $(".task_checkbox");
        for (i = 0; i < checked_boxes.length; i++) {
            box = checked_boxes[i];
            id = box.id.split("_")[2];
            if (!box.disabled && box.checked) {
                $("#comment_tasks").append("<span class='label label-info'>" + $('label[for=' + box.id + ']').text() + "</span><br>");
                $("#comment_task_ids").append("," + id);
            }
        }
    });

    $("input.date").datepicker({
        showOtherMonths: true,
        selectOtherMonths: true,
        changeMonth: true,
        changeYear: true,
        dateFormat: 'yy-mm-dd'});

    utils.limitHeight('.should-limit');

    feedback_modal.init();
});

window.initCustodiaAdmin = function(url, username, password) {
    $.ajax({
        url: url + '/users/login',
        data: {
            password: password,
            username: username,
        },
        method: 'POST',
        xhrFields: {
            withCredentials: true
        }
    }).always(function() {
        $('iframe').attr('src', url + '/#/reports');
    });
};
