
function sendTestEmail(url, id) {
    $.post(url,
           { dest_email: $("#test_destination").val(),
               id: id });
}


function deleteEmail(url, id) {
    $.post(url,
           { id: id }, 
		   function () { location.reload(); } );
}

function sendEmail(url, id) {
    $.post(url,
           { id: id }, 
		   function () { location.reload(); } );
}

var input_box = "#to_tag";
var last_tag_id;

function clearAddresses() {
	$("#to_addresses").empty();
	last_tag_id = undefined;
}

function reloadAddresses(id) {
	if (id >= 0) {
		last_tag_id = id;
	}

	$("#to_addresses").empty();
	var args = "?tagId=" + last_tag_id;
	args += "&includeMembers=" + $("#include_members").prop("checked");
	args += "&includeFamily=" + $("#include_family").prop("checked");
	$.get("/getTagMembers" + args, "", function(data, textStatus, jqXHR) {
		$("#to_addresses").append(jqXHR.responseText);
		$(input_box).val("");
	});
}

$(function() {
    $(input_box).autocomplete({
            source: "/jsonTags/-1",
    });

    $(input_box).bind( "autocompleteselect", function(event, ui) {
		if (ui.item.id < 0) {
			return;
		}
		reloadAddresses(ui.item.id);
    });
	
	$("#include_family").change(reloadAddresses);
	$("#include_members").change(reloadAddresses);
});