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
    createStudent: function (name) {
        ajax.post('/student/create', {
            name: name
        }).then(function (data) {
            dispatcher.dispatch({
                type: constants.systemEvents.FLASH,
                message: 'Successfully created ' + data.made.name + '.'
            });
            router.get().transitionTo('students');
        }, function (error) {
            dispatcher.dispatch({
                type: constants.systemEvents.FLASH,
                message: 'An error occurred during creation.'
            });
        });
    },
    updateStudent: function (id, name) {
        ajax.post('/rename', {
            _id: id,
            name: name
        }).then(function (data) {
            this.loadStudents();
            dispatcher.dispatch({
                type: constants.studentEvents.STUDENT_LOADED,
                data: data.student
            });
        }.bind(this));
    },
    swipeStudent: function (student, direction) {
        var student = student;
        ajax.post('/swipe', {_id: student._id, direction: direction})
            .then(function (data) {
                dispatcher.dispatch({
                    type: constants.studentEvents.STUDENT_SWIPED,
                    data: {students: data}
                });
                this.loadStudent(student._id);
                dispatcher.dispatch({
                    type: constants.systemEvents.FLASH,
                    message: student.name + ' swiped successfully!'
                });
                student = null;
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