var eventEmitter = require('events').EventEmitter,
    constants = require('./appconstants'),
    ajax = require('./ajaxhelper'),
    myhistory = require('./myhistory.js'),
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
    createClass: function (id, name, to_date, from_date, minutes, late_time) {
        var onSave =
                function(message) {
                    return function (data) {
                        dispatcher.dispatch({
                            type: constants.systemEvents.FLASH,
                            message: 'Successfully ' + message + ' class ' + name
                        });
                        dispatcher.dispatch({
                            type: constants.classEvents.CLASS_CREATED,
                            data: data
                        });
                        myhistory.replace('classes');
                    };
                };
        if(id > 0) {
            ajax.post('/classes/'+id, {name: name, to_date:to_date, from_date:from_date, minutes:minutes, late_time:late_time}).then(onSave("edited"));
        } else {
            ajax.post('/classes', {name: name, to_date:to_date, from_date:from_date, minutes:minutes, late_time:late_time}).then(onSave("created"));
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
