var eventEmitter = require('events').EventEmitter,
    constants = require('./appconstants'),
    ajax = require('marmottajax'),
    dispatcher = require('./appdispatcher');

var exports = {
    loadStudents: function () {
        ajax({
            url: '/students',
            json: true
        }).then(function (data) {
            dispatcher.dispatch({
                type: constants.studentEvents.LOADED,
                data: data
            });
        });
    },
    loadStudent: function (id) {
        ajax({
            url: '/student/' + id,
            json: true
        }).then(function (data) {
            dispatcher.dispatch({
                type: constants.studentEvents.STUDENT_LOADED,
                data: data.student
            });
        });
    }
};

module.exports = exports;