var utils = require('./utils');

var last_content = $("#content").val();
var needs_render = true;

function contentChanged() {
    var newContent = $("#content").val();
    if (newContent != last_content) {
        last_content = newContent;
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
           {markdown: last_content},
           function(data) {
        $("#markdown_preview").html(data);
    });
}

window.initEditEntry = function() {
    $("#content").on(utils.TEXT_AREA_EVENTS, contentChanged);
    window.setTimeout(renderContent, 3000);
};
