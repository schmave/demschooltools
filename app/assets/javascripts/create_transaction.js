var Handlebars = require('handlebars');

export function init(cashAccounts, digitalAccounts) {

	var createTransactionTemplate = Handlebars.compile($("#create-transaction-template").html());

	var modeSelectingOption = true;
	$('.create-transaction-option').click(function() {
		if (modeSelectingOption) {
			modeSelectingOption = false;
			$('.create-transaction-option').not(this).hide(300);
			$(this).addClass('selected');
			$('#create-transaction').show().html(renderTransactionCreator($(this).data('type')));
		} else {
			modeSelectingOption = true;
			$('.create-transaction-option').show(300).removeClass('selected');
			$('#create-transaction').hide();
		}
	});

	function renderTransactionCreator(transactionType) {
		var table = $(createTransactionTemplate({transactionType: transactionType}));
		var fromRow = table.find('#create-transaction-from');
		var toRow = table.find('#create-transaction-to');

		if (transactionType === 'CashDeposit') {
			registerAutocomplete(fromRow, cashAccounts);
			registerAutocomplete(toRow, digitalAccounts);
		}
		else if (transactionType === 'CashWithdrawal') {
			registerAutocomplete(fromRow, digitalAccounts);
			registerAutocomplete(toRow, cashAccounts);
		}
		else if (transactionType === 'CashTransfer') {
			registerAutocomplete(fromRow, cashAccounts);
			registerAutocomplete(toRow, cashAccounts);
		}
		else if (transactionType === 'DigitalCredit') {
			registerAutocomplete(toRow, digitalAccounts);
		}
		else if (transactionType === 'DigitalPurchase') {
			registerAutocomplete(fromRow, digitalAccounts);
		}
		else if (transactionType === 'DigitalTransfer') {
			registerAutocomplete(fromRow, digitalAccounts);
			registerAutocomplete(toRow, digitalAccounts);
		}
		else throw 'invalid transaction type: ' + transactionType;

		return table;
	}

	function registerAutocomplete(row, accounts) {
		var textInput = row.find('.js-account-name');
		var idInput = row.find('.js-account-id');
		textInput.autocomplete({
			source: accounts
		});
		textInput.bind("autocompleteselect", function(event, ui) {
			var id = ui.item.id;
			var label = ui.item.label;
			idInput.val(id);
	    });
	}
}