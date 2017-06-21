require('jquery');
var Handlebars = require('handlebars');

var limitHeight = function(selector) {
    $(selector).each(function() {
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
};

window.enableTagBox = function(input_box, destination_div, person_id) {
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
};

var tag_template = Handlebars.compile('\
    <span id="tag-{{num}}" style="white-space: nowrap;"><input type=hidden name=tag_id value={{tag_id}}><span \
        class="label label-success">{{name}} \
       </span><a class="tag_x"><img src="/assets/images/x.png"></a> \
            </span>');

window.enableNoPersonTagBox = function(input_box, destination_div, limit_one) {
    var self = this;
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
        };
    };

    $(input_box).bind( "autocompleteselect", function(event, ui) {
        var new_tag_html =
            $(destination_div).append(
                tag_template({"name": ui.item.label,
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
};

var displayName = function(person) {
    if (!person) {
        return "<No name>";
    }
    if (person.displayName) {
        return person.displayName;
    } else {
        return person.first_name;
    }
};

// reformat a YYYY-MM-DD date string using the given format specifier
var reformatDate = function(format, date_str) {
    if (!date_str) {
        return undefined;
    }

    date = parseDate(date_str);
    return $.datepicker.formatDate(format, date);
};

// parse a date in YYYY-MM-DD format
var parseDate = function(date_str) {
    var parts = date_str.split('-');
    // new Date(year, month [, day [, hours[, minutes[, seconds[, ms]]]]])
    return new Date(parts[0], parts[1]-1, parts[2]); // Note: months are 0-based
};

module.exports = {
    displayName: displayName,
    limitHeight: limitHeight,
    parseDate: parseDate,
    reformatDate: reformatDate,
    // These events should capture all possible ways to change the text
    // in a textfield.
    TEXT_AREA_EVENTS: "change keyup paste cut",
};
