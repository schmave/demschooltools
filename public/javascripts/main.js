next_case_num = 1;
function addCase()
{
	new_case = $("#case_proto").clone();

    new_case.attr("id", "case_" + next_case_num);
    new_case.find(".testifiers").attr("id", "testifiers_"+next_case_num);
    new_case.find(".findings").attr("id", "findings_"+next_case_num);

    enablePersonSearch(new_case.find(".person_search"));

    next_case_num += 1;
    $("#meeting").append(new_case);
	new_case.show();
}

function enablePersonSearch(search_box)
{
    search_box.autocomplete({
        source: "/jsonPeople",
    });

    search_box.siblings(".person_hidden").text("{}");

    search_box.bind( "autocompleteselect", function(event, ui) {
        search_box.siblings(".person_list").append(ui.item.name);
        search_box.siblings(".person_hidden").append(ui.item.id)
    });
}
