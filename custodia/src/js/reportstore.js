const dispatcher = require("./appdispatcher");
const base = require("./storebase");
const constants = require("./appconstants");
const actionCreator = require("./reportactioncreator");

let reports = {};
let schoolYears;

const exports = Object.create(base);

exports.getSchoolYears = function (force) {
  if (!schoolYears || force) {
    actionCreator.loadSchoolYears();
  } else {
    return schoolYears;
  }
};

exports.getReport = function (year) {
  const classId = 2; // This is only needed to communicate with the old backend.
  if (!year || !classId) return null;
  if (reports[year + classId]) {
    return reports[year + classId];
  } else if (!reports[year + classId] || reports[year + classId] === "loading") {
    reports[year + classId] = "loading";
    actionCreator.loadReport(year, classId);
  }
  return null;
};

exports.removeChangeListener = function (callback) {
  this.removeListener(this.CHANGE_EVENT, callback);
  if (this.listeners(this.CHANGE_EVENT).length == 0) {
    reports = {};
  }
};

dispatcher.register(function (action) {
  switch (action.type) {
    case constants.reportEvents.YEARS_LOADED:
      schoolYears = action.data;
      exports.emitChange();
      break;
    case constants.reportEvents.REPORT_LOADED:
      reports = {};
      reports[action.data.year + action.data.classId] = action.data.report;
      exports.emitChange();
      break;
    case constants.reportEvents.PERIOD_CREATED:
      exports.getSchoolYears(true);
      break;
    case constants.reportEvents.PERIOD_DELETED:
      schoolYears = action.data;
      exports.emitChange();
      break;
  }
});

module.exports = exports;
