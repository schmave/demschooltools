const constants = require("./appconstants");
const ajax = require("./ajaxhelper");
const myhistory = require("./myhistory.js");
const dispatcher = require("./appdispatcher");

const exports = {
  loadStudents: function () {
    $.ajax({
      url: "/students",
    }).then(function (data) {
      dispatcher.dispatch({
        type: constants.studentEvents.LOADED,
        data,
      });
    });
  },
  loadStudent: function (id) {
    ajax.get("/students/" + id).then(function (data) {
      dispatcher.dispatch({
        type: constants.studentEvents.STUDENT_LOADED,
        data: data.student,
      });
    });
  },
  swipeStudent: function (student, direction, missing) {
    ajax
      .post("students/" + student._id + "/swipe", {
        direction,
        missing,
      })
      .then(function (data) {
        dispatcher.dispatch({
          type: constants.studentEvents.STUDENT_SWIPED,
          data,
        });
        dispatcher.dispatch({
          type: constants.systemEvents.FLASH,
          message: student.name + " swiped successfully!",
        });
        myhistory.replace("/students");
      });
  },
  markAbsent: function (student) {
    ajax.post("/students/" + student._id + "/absent").then(
      function (data) {
        dispatcher.dispatch({
          type: constants.studentEvents.MARKED_ABSENT,
          data: data.student,
        });
        this.loadStudents();
      }.bind(this),
    );
  },
  deleteSwipe: function (swipe, student) {
    ajax.post("/students/" + student._id + "/swipe/delete", { swipe }).then(function (data) {
      dispatcher.dispatch({
        type: constants.studentEvents.STUDENT_LOADED,
        data: data.student,
      });
    });
  },
  excuse: function (studentId, day) {
    ajax.post("students/" + studentId + "/excuse", { day }).then(
      function () {
        this.loadStudent(studentId);
      }.bind(this),
    );
  },
  override: function (studentId, day) {
    ajax.post("/students/" + studentId + "/override", { day }).then(
      function () {
        this.loadStudent(studentId);
      }.bind(this),
    );
  },
};

module.exports = exports;
