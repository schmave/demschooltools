const constants = require("./appconstants");
const ajax = require("./ajaxhelper");
const myhistory = require("./myhistory.js").default;
const dispatcher = require("./appdispatcher");

export const loadStudents = () => {
  ajax
    .get({
      url: "/students",
    })
    .then(function (data) {
      dispatcher.dispatch({
        type: constants.studentEvents.LOADED,
        data,
      });
    });
};

function loadStudentData(requestData) {
  dispatcher.dispatch({
    type: constants.studentEvents.STUDENT_LOADED,
    data: requestData.student,
  });
}

export const loadStudent = function (id) {
  ajax.get("/students/" + id).then(loadStudentData);
};

export const updateStudent = function (id, start_date, minutes) {
  return ajax
    .put("/students/" + id, {
      start_date,
      minutes,
    })
    .then(loadStudentData);
};

export const swipeStudent = function (student, direction, overrideTime) {
  let overrideDate;
  if (overrideTime) {
    overrideDate =
      direction === "in" ? new Date().toISOString().substr(0, 10) : student.last_swipe_date;
  }
  ajax
    .post("students/" + student._id + "/swipe", {
      direction,
      overrideDate,
      overrideTime,
    })
    .then(function (data) {
      dispatcher.dispatch({
        type: constants.studentEvents.STUDENT_SWIPED,
        data,
      });
      if (overrideTime === undefined) {
        // Don't show this message if we are filling in an old missing swipe.
        dispatcher.dispatch({
          type: constants.systemEvents.FLASH,
          message: student.name + " swiped successfully!",
        });
      }
      myhistory.replace("students");
    });
};
export const markAbsent = function (student) {
  ajax.post("students/" + student._id + "/absent").then(
    function (data) {
      dispatcher.dispatch({
        type: constants.studentEvents.MARKED_ABSENT,
        data: data.student,
      });
      this.loadStudents();
    }.bind(this),
  );
};
export const deleteSwipe = function (swipe, student) {
  ajax.post("/students/" + student._id + "/swipe/delete", { swipe }).then(loadStudentData);
};
export const excuse = function (studentId, day, undo) {
  ajax.post("students/" + studentId + "/excuse", { day, undo }).then(loadStudentData);
};
export const override = function (studentId, day, undo) {
  ajax.post("/students/" + studentId + "/override", { day, undo }).then(loadStudentData);
};
