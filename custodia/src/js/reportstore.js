import constants from "./appconstants";
import dispatcher from "./appdispatcher";
import actionCreator from "./reportactioncreator";
import base from "./storebase";

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

exports.getReport = function (year, filterStudents) {
  const classId = 2; // This is only needed to communicate with the old backend.
  if (!year || !classId) return null;

  const reportKey = year + classId + filterStudents;

  if (reports[reportKey]) {
    return reports[reportKey];
  } else {
    reports[reportKey] = "loading";
    actionCreator.loadReport(year, classId, filterStudents);
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
      reports[action.data.year + action.data.classId + action.data.filterStudents] =
        action.data.report;
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

export default exports;
