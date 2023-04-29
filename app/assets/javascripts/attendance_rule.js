var utils = require('./utils');

export function init(selectedPersonId, people) {

  $('.js-date').datepicker();

  $('.js-time').blur(function() {
    var time = utils.formatTime($(this).val());
    $(this).val(time);
  });

  utils.registerAutocomplete($('.js-person'), people, false, selectedPersonId);
}
