// These events should capture all possible ways to change the text
// in a textfield.
TEXT_AREA_EVENTS = "change keyup paste cut"

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

    $(".comment_text").each(function() {
        if (this.offsetHeight > 80) {
            $(this).addClass("limit_height");
            $(this).after("<a href='#'>more...</a>");
            $(this).next().click(function (event) {
                $(event.target).prev().removeClass("limit_height");
                $(event.target).remove();
                return false;
            });
        }
    });

    $("input.date").datepicker({
        showOtherMonths: true,
        selectOtherMonths: true,
        changeMonth: true,
        changeYear: true,
        dateFormat: 'yy-mm-dd'});
});

function removeTag(el, person_id, tag_id)
{
    el.prev().empty();
    el.empty();
    $.post("/removeTag/" + person_id + "/" + tag_id,
           function(data, textStatus, jqXHR) {}
    );
}

function enableTagBox(input_box, destination_div, person_id) {
    $(input_box).autocomplete({
            source: "/jsonTags/" + person_id,
    });

    $(input_box).bind( "autocompleteselect", function(event, ui) {
        var args = "";
        if (ui.item.id > 0) {
            args = "?tagId=" + ui.item.id;
        } else {
            args = "?title=" + $(input_box).val();
        }
        $.post("/addTag/" + person_id + args, "", function(data, textStatus, jqXHR) {
            $(destination_div).append(jqXHR.responseText);
            $(input_box).val("");
        });
    });
}

function enableNoPersonTagBox(input_box, destination_div, limit_one) {
    var self = this;
    self.tag_template = Handlebars.compile($("#tag-template").html());
    self.tag_count = 0;

    $(input_box).autocomplete({
            source: "/jsonTags/-1",
    });

    var removeTag = function(tag_i) {
        return function() {
            $(destination_div).find("#tag-" + tag_i).empty();

            if (limit_one) {
                $(input_box).show();
            }
        }
    }

    $(input_box).bind( "autocompleteselect", function(event, ui) {
        var new_tag_html =
            $(destination_div).append(
                self.tag_template({"name": ui.item.label,
                    "num": self.tag_count++,
                    "tag_id": ui.item.id}))
                .children(":last-child");

        new_tag_html.find(".tag_x").click(removeTag(self.tag_count - 1));

        $(input_box).val("");

        if (limit_one) {
            $(input_box).hide();
        }
        event.preventDefault();
    });
}

function displayName(person) {
	if (person.displayName) {
		return person.displayName;
	} else {
		return person.first_name;
	}
}

