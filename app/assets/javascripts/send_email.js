
function sendTestEmail(url, id) {
    $("#loading-indicator-test").show();
    $("#send-button-test").prop("disabled", true);
    $.post(url,
           { dest_email: $("#test_destination").val(),
               id: id },
           function() {
                  $("#loading-indicator-test").hide();
                  $("#send-button-test").prop("disabled", false);
             }
               );
}


function deleteEmail(url, id) {
    $.post(url,
           { id: id },
		   function () { location.reload(); } );
}

function sendEmail(url, id) {
    $("#loading-indicator").show();
    $("#send-button").prop("disabled", true);
    $.post(url,
           { id: id,
			 familyMode: $("#family_mode").val(),
			 tagId: last_tag_id,
			 from: $("#source_address").val(),
			 },
		   function () {
                  location.reload();
                  $("#loading-indicator").hide();
                  $("#send-button").prop("disabled", false);
           } );
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
	args += "&familyMode=" + $("#family_mode").val();
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

    $("#family_mode").change(reloadAddresses);
});
