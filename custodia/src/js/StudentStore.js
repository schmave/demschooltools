var EventEmitter = require('events').EventEmitter,
    dispatcher = require('./appdispatcher'),
    assign = require('object-assign'),
    constants = require('./appconstants'),
    actionCreator = require('./studentactioncreator');

var CHANGE_EVENT = 'change';

var students;

function getStudents(){
    if(students){
        return students;
    }else{
        actionCreator.loadStudents();
        return [];
    }
}
var exports = assign({}, EventEmitter.prototype, {
    getStudents: getStudents,
    emitChange: function(){
        this.emit(CHANGE_EVENT);
    },
    addChangeListener: function(callback){
        this.on(CHANGE_EVENT, callback);
    }
});

dispatcher.register(function(action){
    switch(action.type){
        case constants.studentEvents.LOADED:
            students = action.data;
            exports.emitChange();
            break;
    }
});

module.exports = exports;