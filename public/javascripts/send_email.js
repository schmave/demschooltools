
function sendTestEmail(url, id) {
    $.post(url,
           { dest_email: $("#test_destination").val(),
               id: id });
}

