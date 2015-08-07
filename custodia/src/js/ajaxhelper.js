var exports = {
    post: function(url, data){
        return $.ajax({
            url: url,
            method: 'POST',
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify(data)
        });
    },
    get: function(url){
        return $.ajax(url);
    }
};

module.exports = exports;