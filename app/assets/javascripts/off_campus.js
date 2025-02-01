const utils = require('./utils');
const autocomplete = require('./autocomplete');

export function init(people) {
    $('.js-date')
        .val($.datepicker.formatDate('mm/dd/yy', new Date()))
        .datepicker();

    $('.js-person-row').each(function () {
        const startingValues = [];
        const i = $(this).attr('data-index');
        const opts = {
            autoAdvance: true,
            textFieldWidth: 120,
            idFieldName: `personid-${i}`,
        };
        autocomplete.registerAutocomplete(
            $(this),
            people,
            startingValues,
            opts,
        );
    });

    $('.js-time').blur(function () {
        const time = utils.formatTime($(this).val());
        $(this).val(time);
    });
}
