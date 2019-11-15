var utils = require('./utils');

export function init(people) {

	$('.js-date').val($.datepicker.formatDate('mm/dd/yy', new Date())).datepicker();

	$('.js-person-row').each(function() {
		registerAutocomplete($(this), people);
	});
}

function registerAutocomplete(row, people) {
	var selected = row.find('.js-person-name-selected');
    var selectedText = row.find('.js-person-name-selected-text');
	var textInput = row.find('.js-person-name');
    var idInput = row.find('.js-person-id');

	textInput.autocomplete({
        source: people,
        delay: 0,
        autoFocus: true,
    });

    textInput.bind("autocompleteselect", function(event, ui) {
        select(ui.item);
    });

    function select(item) {
        idInput.val(item.id);
        utils.selectNextInput(idInput);
        textInput.hide();
        selectedText.html(item.label);
        selected.show();
    }

    selected.find('img').click(function() {
        selected.hide();
        idInput.val('');
        textInput.val('').show().focus();
    });
}