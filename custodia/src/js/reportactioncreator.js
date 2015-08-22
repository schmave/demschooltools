var eventEmitter = require('events').EventEmitter,
    constants = require('./appconstants'),
    ajax = require('./ajaxhelper'),
    router = require('./routercontainer'),
    dispatcher = require('./appdispatcher');

var exports = {
    loadSchoolYears: function () {
        $.ajax({
            url: '/year/all'
        }).then(function (data) {
            dispatcher.dispatch({
                type: constants.reportEvents.YEARS_LOADED,
                data: data
            });
        });
    },
    loadReport: function(year){
        $.ajax({
            url: '/student/report/' + encodeURIComponent(year)
        }).then(function(data){
            dispatcher.dispatch({
                type: constants.reportEvents.REPORT_LOADED,
                data: {year: year, report: data}
            });
        });
    },
    createPeriod: function(start, end){
        console.log('need to create period for: ', start, end);
    }
};

module.exports = exports;
