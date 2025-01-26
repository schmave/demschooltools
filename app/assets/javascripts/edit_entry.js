const utils = require('./utils');

let last_content = $('#content').val();
let needs_render = true;

function contentChanged() {
    const newContent = $('#content').val();
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

    $.post('/renderMarkdown', { markdown: last_content }, function (data) {
        $('#markdown_preview').html(data);
    });
}

window.initEditEntry = function () {
    $('#content').on(utils.TEXT_AREA_EVENTS, contentChanged);
    window.setTimeout(renderContent, 3000);
};
