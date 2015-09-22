var EventEmitter = require('events').EventEmitter,
    dispatcher = require('./appdispatcher'),
    constants = require('./appconstants'),
    base = require('./storebase'),
    actionCreator = require('./studentactioncreator');

var students, today;
var studentDetails = {};

var exports = Object.create(base);

exports.getStudents = function(force){
    if(!force && students){
        return students;
    }else{
        actionCreator.loadStudents();
        return [];
    }
}

exports.getToday = function(){
    if(today) return today;

    actionCreator.loadToday();
}

exports.getStudent = function(id){
    if(studentDetails[id]){
        return studentDetails[id];
    }else{
        actionCreator.loadStudent(id);
        return null;
    }
}

dispatcher.register(function(action){
    switch(action.type){
        case constants.studentEvents.LOADED:
            students = action.data;
            exports.emitChange();
            break;
        case constants.systemEvents.TODAY_LOADED:
            today = action.data;
            exports.emitChange();
            break;
        case constants.studentEvents.STUDENT_SWIPED:
            students = action.data.students;
            break;
        case constants.studentEvents.STUDENT_LOADED:
        case constants.studentEvents.MARKED_ABSENT:
            studentDetails[action.data._id] = action.data;
            exports.emitChange();
            break;
    }
});

module.exports = exports;
