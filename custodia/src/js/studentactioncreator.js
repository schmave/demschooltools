var eventEmitter = require('events').EventEmitter,
    constants = require('./appconstants'),
    ajax = require('./ajaxhelper'),
    router = require('./routercontainer'),
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
        ajax.get('/student/' + id)
            .then(function (data) {
                dispatcher.dispatch({
                    type: constants.studentEvents.STUDENT_LOADED,
                    data: data.student
                });
            });
    },
    swipeStudent: function (student, direction) {
        ajax.post('/swipe', {_id: student._id, direction: direction})
            .then(function (data) {
                dispatcher.dispatch({
                    type: constants.studentEvents.STUDENT_SWIPED,
                    data: data
                });
                this.loadStudents();
                router.get().transitionTo('students');
            }.bind(this));
    },
    markAbsent: function (student) {
        ajax.post('/student/toggleabsent', {_id: student._id})
            .then(function (data) {
                dispatcher.dispatch({
                    type: constants.studentEvents.MARKED_ABSENT,
                    data: data.student
                });
            });

    }
};

module.exports = exports;