var EventEmitter = require('events').EventEmitter,
    assign = require('object-assign'),
    dispatcher = require('./appdispatcher'),
    constants = require('./appconstants'),
    actionCreator = require('./reportactioncreator'),
    ajax = require('./ajaxhelper');

var isAdmin;
var CHANGE_EVENT = "CHANGE!";
var schoolYears, report, currentYear, loading;

var exports = assign({}, EventEmitter.prototype, {
    getSchoolYears: function(){
        if(!schoolYears){
            actionCreator.loadSchoolYears();
        }else {
            return schoolYears;
        }
    },
    getReport: function(year){
        if(currentYear === year && report){
            return report;
        }else if(!loading){
            report = null;
            loading = true;
            currentYear = year;
            actionCreator.loadReport(year);
        }
        return [];
    },
    emitChange: function(){
        this.emit(CHANGE_EVENT);
    },
    addChangeListener: function(callback){
        this.on(CHANGE_EVENT, callback);
    },
    removeChangeListener: function(callback){
        this.removeListener(CHANGE_EVENT, callback);
    }
});

dispatcher.register(function(action){
    switch(action.type){
        case constants.reportEvents.YEARS_LOADED:
            schoolYears = action.data;
            exports.emitChange();
            break;
        case constants.reportEvents.REPORT_LOADED:
            report = action.data;
            loading = false;
            exports.emitChange();
            break;
    }
});

module.exports = exports;