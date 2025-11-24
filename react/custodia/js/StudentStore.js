import constants from "./appconstants.js";
import dispatcher from "./appdispatcher.js";
import base from "./storebase.js";
import * as actionCreator from "./studentactioncreator.js";

let students;
const studentDetails = {};

const exports = Object.create(base);

exports.getStudents = function (force) {
  if (!force && students) {
    return students;
  } else {
    actionCreator.loadStudents();
    return [];
  }
};

exports.getStudent = function (id, force) {
  if (!force && studentDetails[id]) {
    return studentDetails[id];
  } else {
    actionCreator.loadStudent(id);
    return null;
  }
};

dispatcher.register(function (action) {
  switch (action.type) {
    case constants.studentEvents.LOADED:
      students = action.data.students;
      exports.emitChange();
      break;
    case constants.studentEvents.STUDENT_SWIPED:
      students = action.data.students;
      exports.emitChange();
      break;
    case constants.studentEvents.STUDENT_LOADED:
    case constants.studentEvents.MARKED_ABSENT:
      studentDetails[action.data._id] = action.data;
      exports.emitChange();
      break;
  }
});

export default exports;
