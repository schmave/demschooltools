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

