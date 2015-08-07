var ajax = require('./ajaxhelper');

var isAdmin;

ajax.get('/user/is-admin').then(function (data) {
    isAdmin = true;
}, function (data) {
    isAdmin = false
});

var exports = {
    isAdmin: function () {
        return isAdmin;
    }
};

module.exports = exports;