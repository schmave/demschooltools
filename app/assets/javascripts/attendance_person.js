function checkAll() {
    $('input[type=checkbox]').prop('checked', true);
    $('input[type=checkbox]').change();
}

function uncheckAll() {
    $('input[type=checkbox]').prop('checked', false);
    $('input[type=checkbox]').change();
}

function checkboxChange() {
    const checked = $(this).prop('checked');
    const code = $(this).data('code');
    if (checked) {
        $('tr.code-' + code).show();
    } else {
        $('tr.code-' + code).hide();
    }

    if ($('input[type=checkbox]:not(:checked)').length > 0) {
        $('.friday-row').addClass('hide-friday');
    } else {
        $('.hide-friday').removeClass('hide-friday');
    }
}

window.initAttendancePerson = function () {
    $('#check-all').click(checkAll);
    $('#uncheck-all').click(uncheckAll);
    $('input[type=checkbox]').change(checkboxChange);
};
