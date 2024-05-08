const constants = require("./appconstants");
const ajax = require("./ajaxhelper");
const dispatcher = require("./appdispatcher");

const exports = {
  loadSchoolYears: function () {
    ajax
      .get({
        url: "/reports/years",
      })
      .then(function (data) {
        dispatcher.dispatch({
          type: constants.reportEvents.YEARS_LOADED,
          data,
        });
      });
  },
  loadReport: function (year, classId) {
    const classRouteId = classId ? "/" + classId : "";
    ajax
      .get({
        url: "/reports/" + encodeURIComponent(year) + classRouteId,
      })
      .then(function (data) {
        dispatcher.dispatch({
          type: constants.reportEvents.REPORT_LOADED,
          data: { year, report: data, classId },
        });
      });
  },
  createPeriod: function (start, end) {
    ajax.post("/reports/years", { from_date: start, to_date: end }).then(function (data) {
      const period = data.made.name.split(" ");
      dispatcher.dispatch({
        type: constants.systemEvents.FLASH,
        message: "Successfully created period from " + period[0] + " to " + period[1],
      });
      dispatcher.dispatch({
        type: constants.reportEvents.PERIOD_CREATED,
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
        data,
      });
    });
  },
};

module.exports = exports;
