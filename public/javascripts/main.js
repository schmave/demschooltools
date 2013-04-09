$(document).ready(function () {
    $( "#same_family_name" ).autocomplete({
        source: all_people
    });

    $( "#same_family_name" ).bind( "autocompleteselect", function(event, ui) {
        $("#same_family_id").val(ui.item.id);
    });
});

