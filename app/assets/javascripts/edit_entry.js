var utils = require('./utils');

var last_content = $("#content").val();
var needs_render = true;

function contentChanged() {
    new_content = $("#content").val();
    if (new_content != last_content) {
        last_content = new_content;
        needs_render = true;
    }
}

function renderContent() {
    window.setTimeout(renderContent, 3000);

    if (!needs_render) {
        return;
    }
    needs_render = false;

    $.post("/renderMarkdown",
           {markdown : last_content},
           function(data) {
        $("#markdown_preview").html(data);
    });
}

window.initEditEntry = function() {
    $("#content").on(utils.TEXT_AREA_EVENTS, contentChanged);
    window.setTimeout(renderContent, 3000);
};