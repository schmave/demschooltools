$(document).ready(function () {
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
});

function enableTagBox(input_box, destination_div, person_id) {
    $(input_box).autocomplete({
            source: "/jsonTags/" + person_id,
    });

    $(input_box).bind( "autocompleteselect", function(event, ui) {
        var args = "";
        var tag_title = ui.item.label;
        if (ui.item.id > 0) {
            args = "?tagId=" + ui.item.id;
        } else {
            tag_title = $(input_box).val();
            args = "?title=" + tag_title;
        }
        $.post("/addTag/" + person_id + args, "", function(data, textStatus, jqXHR) {
            $(destination_div).append("<span class='person_tag'>" + tag_title + "</span> ");
            $(input_box).val("");
        });
    });
}
