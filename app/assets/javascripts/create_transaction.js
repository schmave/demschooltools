const Handlebars = require('handlebars');
const utils = require('./utils');

export function init(accounts) {
    const createTransactionTemplate = Handlebars.compile(
        $('#create-transaction-template').html(),
    );

    let modeSelectingOption = true;
    $('.create-transaction-option').click(function () {
        if (modeSelectingOption) {
            modeSelectingOption = false;
            $('.create-transaction-option').not(this).hide(300);
            $(this).addClass('selected');
            $('#create-transaction')
                .show()
                .html(renderTransactionCreator($(this).data('type')));
        } else {
            modeSelectingOption = true;
            $('.create-transaction-option').show(300).removeClass('selected');
            $('#create-transaction').hide();
        }
    });

    $('body').on('input', '#amount', function () {
        if (Number($('#balance').data('value')) - Number($(this).val()) < 0) {
            $('#balance').addClass('warning');
        } else {
            $('#balance').removeClass('warning');
        }
    });

    function renderTransactionCreator(transactionType) {
        const table = $(createTransactionTemplate({ transactionType }));
        const from = table.find('#create-transaction-from');
        const to = table.find('#create-transaction-to');
        const toRow = table.find('#create-transaction-to-row');
        const date = table.find('#create-transaction-date');

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

        date.val($.datepicker.formatDate('mm/dd/yy', new Date())).datepicker();

        return table;
    }

    function registerAutocomplete(row, accounts, isFromDigitalAccount) {
        const selected = row.find('.js-account-name-selected');
        const selectedText = row.find('.js-account-name-selected-text');
        const textInput = row.find('.js-account-name');
        const idInput = row.find('.js-account-id');

        textInput.autocomplete({
            source: accounts,
            delay: 0,
            autoFocus: true,
        });
        textInput.bind('autocompleteselect', function (event, ui) {
            select(ui.item);
        });

        function select(item) {
            idInput.val(item.id);
            utils.selectNextInput(idInput);
            textInput.hide();
            selectedText.html(item.label);
            if (isFromDigitalAccount) {
                $('#balance')
                    .data('value', item.balance)
                    .html(item.label + "'s current balance: $" + item.balance);
                if (Number(item.balance) - Number($('#amount').val()) < 0) {
                    $('#balance').addClass('warning');
                }
            }
            selected.show();
        }

        selected.find('img').click(function () {
            selected.hide();
            idInput.val('');
            textInput.val('').show().focus();
            if (isFromDigitalAccount) {
                $('#balance').html('').removeClass('warning');
            }
        });
    }
}
