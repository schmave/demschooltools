next_case_num = 1;
function addCase()
{
	new_case = $("#case_proto").clone();
	new_case.attr("id", "case_" + next_case_num);
	next_case_num += 1;
    $("#meeting").append(new_case);
	new_case.show();
}

