const utils = require('./utils');
const autocomplete = require('./autocomplete');

export function init(selectedPersonId, selectedPersonName, people) {
  $('.js-date').datepicker();

  $('.js-time').blur(function() {
    const time = utils.formatTime($(this).val());
    $(this).val(time);
  });

  const container = document.getElementById('attendance-edit-rule-person');
  const startingValues = [{ id: selectedPersonId, label: selectedPersonName }];
  const opts = {
    idFieldName: 'personId',
    textFieldSize: 15
  };
  autocomplete.registerAutocomplete(container, people, startingValues, opts);
}
