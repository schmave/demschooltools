const utils = require('./utils');
const autocomplete = require('./autocomplete');

export function init(selectedPersonId, people) {
  $('.js-date').datepicker();

  $('.js-time').blur(function() {
    const time = utils.formatTime($(this).val());
    $(this).val(time);
  });

  const container = document.getElementById('attendance-edit-rule-person');
  const person = people.filter(p => Number(p.id) === selectedPersonId)[0];
  const personName = person ? person.label : null;
  const startingValues = [{ id: selectedPersonId, label: personName }];
  const opts = {
    idFieldName: 'personId',
    textFieldSize: 15
  };
  autocomplete.registerAutocomplete(container, people, startingValues, opts);
}
