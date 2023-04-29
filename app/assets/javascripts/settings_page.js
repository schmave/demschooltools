export function init() {
  $('.has-dependents').each(function(i, el) {
    const checkboxWithDependents = $(el);
    manageDependents(checkboxWithDependents);
    checkboxWithDependents.click(function() {
      manageDependents(checkboxWithDependents);
    });
  });

  function manageDependents(checkboxWithDependents) {
    if (checkboxWithDependents.prop('checked')) {
      cascadingEnable(checkboxWithDependents);
    } else {
      cascadingDisable(checkboxWithDependents);
    }
  }

  function cascadingEnable(checkboxWithDependents) {
    const dependentElements = findDependentElements(checkboxWithDependents);
    dependentElements.removeClass('dependents-disabled');
    dependentElements.find('input').prop('disabled', false);
    dependentElements.find('.has-dependents').each(function(i, el) {
      if ($(el).prop('checked')) {
        cascadingEnable($(el));
      }
    });
  }

  function cascadingDisable(checkboxWithDependents) {
    const dependentElements = findDependentElements(checkboxWithDependents);
    dependentElements.addClass('dependents-disabled');
    dependentElements.find('input').prop('disabled', true);
    dependentElements.find('.has-dependents').each(function(i, el) {
      cascadingDisable($(el));
    });
  }

  function findDependentElements(checkboxWithDependents) {
    return $('.' + checkboxWithDependents.data('dependent-class'));
  }
}
