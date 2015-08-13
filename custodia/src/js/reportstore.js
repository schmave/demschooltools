var EventEmitter = require('events').EventEmitter,
    assign = require('object-assign'),
    dispatcher = require('./appdispatcher'),
    constants = require('./appconstants'),
    actionCreator = require('./reportactioncreator'),
    ajax = require('./ajaxhelper');

var isAdmin;
var CHANGE_EVENT = "CHANGE!";
var reports = {};
var schoolYears;

var exports = assign({}, EventEmitter.prototype, {
    getSchoolYears: function(){
        if(!schoolYears){
            actionCreator.loadSchoolYears();
        }else {
            return schoolYears;
        }
    },
    getReport: function(year){
        if(!year) return [];
        if(reports[year]){
            return reports[year];
        }else if(!reports[year] || reports[year] === 'loading'){
            reports[year] = 'loading';
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
        if(this.listeners(CHANGE_EVENT).length == 0){
            reports = {};
        }
    }
});

dispatcher.register(function(action){
    switch(action.type){
        case constants.reportEvents.YEARS_LOADED:
            schoolYears = action.data;
            exports.emitChange();
            break;
        case constants.reportEvents.REPORT_LOADED:
            reports[action.data.year] = action.data.report;
            exports.emitChange();
            break;
    }
});

module.exports = exports;