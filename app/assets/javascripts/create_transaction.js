var Handlebars = require('handlebars');

// var accountInputString = '<input type="text" />';
// var accountInputTemplate = Handlebars.compile(accountInputString);

function renderCashAccountInput(container, autocompleteSource) {
	var input = $('<input type="text" />');
	container.html(input);
	input.autocomplete({
		source: autocompleteSource
	});
	input.bind("autocompleteselect", function(event, ui) {
		var id = ui.item.id;
		var label = ui.item.label;
        // self.search_box.val('');
        // event.preventDefault(); // keep jquery from inserting name into textbox
    });
}

export function init() {
	var source = app.cashAccounts;
}