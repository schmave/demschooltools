const Handlebars = require('handlebars');
const utils = require('./utils');

function registerAutocomplete(container, source, startingValues, opts) {
    startingValues = startingValues || [];
    const items = [];

    for (const value of startingValues) {
        const item = registerAutocompleteItem(container, source, opts, onSelect);
        items.push(item);
        item.setValue(value);
    }

    if (!startingValues.length) {
        items.push(registerAutocompleteItem(container, source, opts, onSelect));
    }

    function onSelect() {
        if (opts.multi && !anyBlanks()) {
            items.push(registerAutocompleteItem(container, source, opts, onSelect));
        }
    }

    function anyBlanks() {
        return items.some(item => isBlank(item));
    }

    function isBlank(item) {
        if (opts.allowPlainText) {
            return !item.getId() && !item.getLabel();
        } else {
            return !item.getId();
        }
    }

    return () => {
        return items.filter(item => !isBlank(item)).map(item => {
            return {
                label: item.getLabel(),
                id: item.getId()
            };
        });
    }
}

function registerAutocompleteItem(container, source, opts, onSelect) {
    const div = $('<div class="autocomplete-item"></div>');
    $(container).append(div);

    const template = Handlebars.compile($('#autocomplete-template').html());
    div.html(template({
        textFieldSize: opts.textFieldSize,
        textFieldClass: opts.textFieldClass,
        textFieldName: opts.textFieldName,
        idFieldName: opts.idFieldName
    }));

    const selected = $(div).find('.autocomplete-selected');
    const removeButton = selected.find('img');
    const selectedText = $(div).find('.autocomplete-selected-text');
    const textInput = $(div).find('.autocomplete-text');
    const idInput = $(div).find('.autocomplete-id');

    textInput.autocomplete({
        source,
        delay: 0,
        autoFocus: true,
    });

    textInput.on('autocompleteselect', function(event, ui) {
        select(ui.item);
    });

    if (opts.allowPlainText) {
        textInput.on('input', onSelect);
    }

    removeButton.on('click', function() {
        selected.hide();
        idInput.val('');
        textInput.val('').show().focus();
    });

    function select(value) {
        selectedText.html(value.label);
        if (value.id) {
            idInput.val(value.id);
            onSelect();
            if (opts.autoAdvance) {
                utils.selectNextInput(idInput);
            }
            textInput.hide();
            selected.show();
        }
    }

    return {
        getLabel: () => textInput.val(),
        getId: () => idInput.val(),
        setValue: select
    };
}

module.exports = {
    registerAutocomplete
};
