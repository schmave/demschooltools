var eventEmitter = require("events").EventEmitter,
  constants = require("./appconstants"),
  ajax = require("./ajaxhelper"),
  dispatcher = require("./appdispatcher");

var exports = {
  loadSchoolYears: function () {
    ajax
      .get({
        url: "/reports/years",
      })
      .then(function (data) {
        dispatcher.dispatch({
          type: constants.reportEvents.YEARS_LOADED,
          data: data,
        });
      });
  },
  loadReport: function (year, classId) {
    var classRouteId = classId ? "/" + classId : "";
    ajax
      .get({
        url: "/reports/" + encodeURIComponent(year) + classRouteId,
      })
      .then(function (data) {
        dispatcher.dispatch({
          type: constants.reportEvents.REPORT_LOADED,
          data: { year: year, report: data, classId: classId },
        });
      });
  },
  createPeriod: function (start, end) {
    ajax.post("/reports/years", { from_date: start, to_date: end }).then(function (data) {
      var period = data.made.name.split(" ");
      dispatcher.dispatch({
        type: constants.systemEvents.FLASH,
        message: "Successfully created period from " + period[0] + " to " + period[1],
      });
      dispatcher.dispatch({
        type: constants.reportEvents.PERIOD_CREATED,
        data: data,
      });
    });
  },
  deletePeriod: function (period) {
    ajax.delete("/reports/years/" + encodeURIComponent(period)).then(function (data) {
      dispatcher.dispatch({
        type: constants.systemEvents.FLASH,
        message: "Deleted period " + period,
      });
      dispatcher.dispatch({
        type: constants.reportEvents.PERIOD_DELETED,
        data: data,
      });
    });
  },
};

module.exports = exports;
