import ajax from "./ajaxhelper.js";
import constants from "./appconstants.js";
import dispatcher from "./appdispatcher.js";

export const loadStudents = async () => {
  const data = await ajax.get({
    url: "/students",
  });
  dispatcher.dispatch({
    type: constants.studentEvents.LOADED,
    data,
  });
};

function loadStudentData(requestData) {
  dispatcher.dispatch({
    type: constants.studentEvents.STUDENT_LOADED,
    data: requestData.student,
  });
}

export const loadStudent = async function (id) {
  const requestData = await ajax.get("/students/" + id);
  loadStudentData(requestData);
};

export const updateStudent = async function (id, start_date, minutes) {
  const requestData = await ajax.put("/students/" + id, {
    start_date,
    minutes,
  });
  loadStudentData(requestData);
};

export const swipeStudent = async function (student, direction, overrideTime, navigate) {
  let overrideDate;
  if (overrideTime) {
    overrideDate =
      direction === "in" ? new Date().toISOString().substr(0, 10) : student.last_swipe_date;
  }
  const data = await ajax.post("students/" + student._id + "/swipe", {
    direction,
    overrideDate,
    overrideTime,
  });
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
  navigate?.("/students", { replace: true });
};

export const markAbsent = async function (student) {
  const data = await ajax.post("/students/" + student._id + "/absent");
  dispatcher.dispatch({
    type: constants.studentEvents.MARKED_ABSENT,
    data: data.student,
  });
  loadStudents();
};

export const deleteSwipe = async function (swipe, student) {
  const requestData = await ajax.post("/students/" + student._id + "/swipe/delete", { swipe });
  loadStudentData(requestData);
};

export const excuse = async function (studentId, day, undo) {
  const requestData = await ajax.post("students/" + studentId + "/excuse", { day, undo });
  loadStudentData(requestData);
};

export const override = async function (studentId, day, undo) {
  const requestData = await ajax.post("/students/" + studentId + "/override", { day, undo });
  loadStudentData(requestData);
};
