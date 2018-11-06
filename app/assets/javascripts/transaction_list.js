export function init() {
    var login_message_shown = false;

    $('#hide-archived').click(function() {
        var archivedRows = $('.js-archived');
        if ($(this).is(':checked')) {
            archivedRows.addClass('js-archived-hidden');
            archivedRows.hide(400);
        } else {
            archivedRows.removeClass('js-archived-hidden');
            archivedRows.not('.js-non-personal-hidden').show(400);
        }
    });

    $('#hide-non-personal').click(function() {
        var nonPersonalRows = $('.js-non-personal');
        if ($(this).is(':checked')) {
            nonPersonalRows.addClass('js-non-personal-hidden');
            nonPersonalRows.hide(400);
        } else {
            nonPersonalRows.removeClass('js-non-personal-hidden');
            nonPersonalRows.not('.js-archived-hidden').show(400);
        }
    });

    $('.js-archive').click(function() {
        var checkbox = $(this);
        var id = Number(checkbox.data('id'));
        var row = checkbox.parents('.js-archivable');

        checkbox.prop("disabled", true);

        $.post('/accounting/toggleTransactionArchived/' + id)
            .done(function(data) {
                // We expect an empty response. If it is not empty, the user
                // probably got redirected to the login page.
                if (data !== "") {
                    if (!login_message_shown) {
                        alert("Your change was not saved. You may not be logged in.");
                    }
                    login_message_shown = true;
                    return;
                }
                row.toggleClass('js-archived accounting-archived');
            })
            .always(function() {
                checkbox.prop("disabled", false);
            });
        });
}
