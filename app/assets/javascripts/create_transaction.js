var Handlebars = require('handlebars');
var utils = require('./utils');

export function init(accounts) {
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

    $('body').on('input', '#amount', function() {
        if (Number($('#balance').data('value')) - Number($(this).val()) < 0) {
            $('#balance').addClass('warning');
        } else {
            $('#balance').removeClass('warning');
        }
    });

    function renderTransactionCreator(transactionType) {
        var table = $(createTransactionTemplate({transactionType: transactionType}));
        var from = table.find('#create-transaction-from');
        var to = table.find('#create-transaction-to');
        var toRow = table.find('#create-transaction-to-row');

        if (transactionType === 'CashDeposit') {
            registerAutocomplete(to, accounts);
        } else if (transactionType === 'CashWithdrawal') {
            registerAutocomplete(from, accounts, true);
            toRow.hide();
        } else if (transactionType === 'DigitalTransaction') {
            registerAutocomplete(from, accounts, true);
            registerAutocomplete(to, accounts);
        } else {
            throw new Error('invalid transaction type: ' + transactionType);
        }

        return table;
    }

    function registerAutocomplete(row, accounts, isFromDigitalAccount) {
        var selected = row.find('.js-account-name-selected');
        var selectedText = row.find('.js-account-name-selected-text');
        var textInput = row.find('.js-account-name');
        var idInput = row.find('.js-account-id');

        textInput.autocomplete({
            source: accounts,
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
            if (isFromDigitalAccount) {
                $('#balance').data('value', item.balance).html(item.label + '\'s current balance: $' + item.balance);
                if (Number(item.balance) - Number($('#amount').val()) < 0) {
                    $('#balance').addClass('warning');
                }
            }
            selected.show();
        }

        selected.find('img').click(function() {
            selected.hide();
            idInput.val('');
            textInput.val('').show().focus();
            if (isFromDigitalAccount) {
                $('#balance').html('').removeClass('warning');
            }
        });
    }
}
