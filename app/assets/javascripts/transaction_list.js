export function init() {
    var login_message_shown = false;

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
