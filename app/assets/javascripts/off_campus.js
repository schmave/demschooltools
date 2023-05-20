const utils = require('./utils');

export function init(people) {
  $('.js-date').val($.datepicker.formatDate('mm/dd/yy', new Date())).datepicker();

  $('.js-person-row').each(function() {
    utils.registerAutocomplete($(this), people, true);
  });

  $('.js-time').blur(function() {
    const time = utils.formatTime($(this).val());
    $(this).val(time);
  });
}
