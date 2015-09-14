var eventEmitter = require('events').EventEmitter,
    constants = require('./appconstants'),
    ajax = require('./ajaxhelper'),
    router = require('./routercontainer'),
    dispatcher = require('./appdispatcher');

var exports = {
    loadClasses: function () {
        ajax.get({
            url: '/classes'
        }).then(function (data) {
            dispatcher.dispatch({
                type: constants.reportEvents.CLASSES_LOADED,
                data: data
            });
        });
    },
    createClass: function (name) {
        ajax.post('/classes', {name: name})
            .then(function (data) {
                var period = data.made.name.split(' ');
                dispatcher.dispatch({
                    type: constants.systemEvents.FLASH,
                    message: 'Successfully created class ' + name
                });
                dispatcher.dispatch({
                    type: constants.classEvents.CLASS_CREATED,
                    data: data
                });
            });
    },
    deleteStudentFromClass: function (studentId, classId) {
        ajax.post('/classes/'+classId+'/student/'+studentId+'/delete', {})
            .then(function (data) {
                dispatcher.dispatch({
                    type: constants.systemEvents.FLASH,
                    message: 'Removed student'
                });
                dispatcher.dispatch({
                    type: constants.classEvents.CLASS_STUDENT_DELETED,
                    data: data
                });
            });
    },
    addStudentToClass: function (studentId, classId) {
        ajax.post('/classes/'+classId+'/student/'+studentId+'/add', {})
            .then(function (data) {
                dispatcher.dispatch({
                    type: constants.systemEvents.FLASH,
                    message: 'Added student'
                });
                dispatcher.dispatch({
                    type: constants.classEvents.CLASS_STUDENT_ADDED,
                    data: data
                });
            });
    },
};

module.exports = exports;
