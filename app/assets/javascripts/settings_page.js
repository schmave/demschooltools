
export function init() {

	$('#show-attendance').click(function() {
		var dependsOnAttendance = $('.depends-on-attendance');
		var dependsOnPartialDays = $('.depends-on-attendance-partial-days');
		var inputsDependentOnAttendance = dependsOnAttendance.find('input');
		var inputsDependentOnPartialDays = dependsOnPartialDays.find('input');
		if ($(this).prop('checked')) {
			dependsOnAttendance.removeClass('disabled');
			inputsDependentOnAttendance.prop('disabled', false);
			if ($('#attendance-enable-partial-days').prop('checked')) {
				dependsOnPartialDays.removeClass('disabled');
				inputsDependentOnPartialDays.prop('disabled', false);
			}
		} else {
			dependsOnAttendance.addClass('disabled');
			dependsOnPartialDays.addClass('disabled');
			inputsDependentOnAttendance.prop('disabled', true);
			inputsDependentOnPartialDays.prop('disabled', true);
		}
	});

	$('#attendance-enable-partial-days').click(function() {
		var dependentElement = $('.depends-on-attendance-partial-days');
		var inputs = dependentElement.find('input');
		if ($(this).prop('checked')) {
			dependentElement.removeClass('disabled');
			inputs.prop('disabled', false);
		} else {
			dependentElement.addClass('disabled');
			inputs.prop('disabled', true);
		}
	});
}