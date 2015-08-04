var eventEmitter = require('events').EventEmitter,
    constants = require('./appconstants'),
    dispatcher = require('./appdispatcher');

var exports = {
    loadStudents: function () {
        $.ajax({
            url: '/students'
        }).then(function (data) {
            dispatcher.dispatch({
                type: constants.studentEvents.LOADED,
                data: data
            });
        });
    },
    loadStudent: function (id) {
        $.ajax({
            url: '/student/' + id
        }).then(function (data) {
            dispatcher.dispatch({
                type: constants.studentEvents.STUDENT_LOADED,
                data: data.student
            });
        });
    },
    swipeStudent: function (student, direction) {
        $.ajax({
            url: '/swipe',
            method: 'POST',
            data: {
                _id: student._id,
                direction: direction
            }
        }).then(function (data) {
            dispatcher.dispatch({
                type: constants.studentEvents.STUDENT_SWIPED,
                data: data
            })
        });
    }
};

module.exports = exports;