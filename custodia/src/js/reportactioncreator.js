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
    loadReport: function (year) {
        $.ajax({
            url: '/student/report/' + encodeURIComponent(year)
        }).then(function (data) {
            dispatcher.dispatch({
                type: constants.reportEvents.REPORT_LOADED,
                data: {year: year, report: data}
            });
        });
    },
    createPeriod: function (start, end) {
        ajax.post('/year', {from_date: start, to_date: end})
            .then(function (data) {
                var period = data.made.name.split(' ');
                dispatcher.dispatch({
                    type: constants.systemEvents.FLASH,
                    message: 'Successfully created period from ' + period[0] + ' to ' + period[1]
                });
                dispatcher.dispatch({
                    type: constants.reportEvents.PERIOD_CREATED,
                    data: data
                });
            });
    }
};

module.exports = exports;
