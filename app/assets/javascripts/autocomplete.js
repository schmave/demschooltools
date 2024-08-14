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

    function onSelect() {
        if (opts.multi && !anyBlanks()) {
            items.push(registerAutocompleteItem(container, source, opts, onSelect));
        }
    }

    function anyBlanks() {
        return items.some(item => isBlank(item));
    }

    function isBlank(item) {
        const value = item();
        if (opts.allowPlainText) {
            return !value.id && !value.label;
        } else {
            return !value.id;
        }
    }

    return () => {
        return items.filter(item => !isBlank(item)).map(item => item());
    }
}

function registerAutocompleteItem(container, source, opts, onSelect) {
    const div = document.createElement('div');
    div.classList.add('autocomplete-item');
    const template = Handlebars.compile($('#autocomplete-template').html());
    div.innerHTML = template({
        textFieldWidth: opts.textFieldWidth ? `${opts.textFieldWidth}px` : '',
        textFieldName: opts.textFieldName,
        idFieldName: opts.idFieldName
    });
    container.append(div);

    const selected = container.find('.autocomplete-selected');
    const removeButton = selected.find('img');
    const selectedText = container.find('.autocomplete-selected-text');
    const textInput = container.find('.autocomplete-text');
    const idInput = container.find('.autocomplete-id');

    textInput.autocomplete({
        source,
        delay: 0,
        autoFocus: true,
    });

    textInput.bind("autocompleteselect", function(event, ui) {
        select(ui.item);
    });

    if (opts.allowPlainText) {
        textInput.addEventListener('input', onSelect);
    }

    removeButton.click(function() {
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

    return () => {
        return {
            label: textInput.value,
            id: idInput.value,
            setValue: value => select(value)
        };
    }
}

module.exports = {
    registerAutocomplete
};
