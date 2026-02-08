
export const init = function () {
    $('.feedback_open').click((e) => {
        $('#feedback-dialog').dialog({
            resizable: false,
            width: 500,
            height: 500,
            modal: true,
        });
    });
};
