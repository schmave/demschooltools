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
                type: constants.classEvents.CLASSES_LOADED,
                data: data
            });
        });
    },
    createClass: function (id, name, to_date, from_date, minutes) {
        var onSave = function (data) {
            dispatcher.dispatch({
                type: constants.systemEvents.FLASH,
                message: 'Successfully created class ' + name
            });
            dispatcher.dispatch({
                type: constants.classEvents.CLASS_CREATED,
                data: data
            });
            router.get().transitionTo('classes');
        };
        if(id > 0) {
            ajax.post('/classes/'+id, {name: name, to_date:to_date, from_date:from_date, minutes:minutes}).then(onSave);
        } else {
            ajax.post('/classes', {name: name, to_date:to_date, from_date:from_date, minutes:minutes}).then(onSave);
        }
    },
    deleteStudentFromClass: function (studentId, classId) {
        ajax.post('/classes/'+classId+'/student/'+studentId+'/delete', {})
            .then(function (data) {
                dispatcher.dispatch({
                    type: constants.classEvents.CLASS_STUDENT_DELETED,
                    data: data
                });
            });
    },
    activateClass: function (classId) {
        ajax.post('/classes/'+classId+'/activate', {})
            .then(function (data) {
                dispatcher.dispatch({
                    type: constants.classEvents.CLASS_CHANGED,
                    data: data
                });
            });
    },
    addStudentToClass: function (studentId, classId) {
        ajax.post('/classes/'+classId+'/student/'+studentId+'/add', {})
            .then(function (data) {
                dispatcher.dispatch({
                    type: constants.classEvents.CLASS_STUDENT_ADDED,
                    data: data
                });
            });
    },
};

module.exports = exports;
