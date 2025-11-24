import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import studentStore from "./StudentStore";
import { useSwipeLogic } from "./hooks/useSwipeLogic.js";
import SwipeHelpers from "./swipeHelpers.jsx";

export default function StudentTable() {
  const navigate = useNavigate();
  const [students, setStudents] = useState(() => studentStore.getStudents(true));
  const {
    swipeState,
    validateSignDirection,
    handleSwipeComplete,
    handleSwipeCancel,
    updateMissingTime,
  } = useSwipeLogic();

  useEffect(() => {
    const onChange = () => {
      setStudents(studentStore.getStudents());
    };

    studentStore.addChangeListener(onChange);

    return () => {
      studentStore.removeChangeListener(onChange);
    };
  }, []);

  const signIn = (student) => {
    validateSignDirection(student, "in");
  };

  const signOut = (student) => {
    validateSignDirection(student, "out");
  };

  const isSigningIn = (student) => {
    return student.last_swipe_type === "out" || !student.in_today;
  };

  const getSwipeButton = (student, way) => {
    let buttonIcon = "fa-arrow-right";
    if (way === "out") {
      buttonIcon = "fa-arrow-left";
    }
    const iclassName = "fa " + buttonIcon + " sign-" + student._id;
    const is_teacher_class = student.is_teacher ? " is_teacher" : "";
    const button_class = "btn-default name-button" + (student.swiped_today_late ? " late" : "");
    const sign_function = isSigningIn(student) ? signIn : signOut;
    if (way === "out") {
      return (
        <button
          onClick={sign_function.bind(null, student)}
          className={"btn btn-sm " + button_class + is_teacher_class}
        >
          <i className={iclassName}>&nbsp;</i>
          <span className="name-span">{student.name}</span>
        </button>
      );
    } else {
      return (
        <button
          onClick={sign_function.bind(null, student)}
          className={"btn btn-sm " + button_class + is_teacher_class}
        >
          <span className="name-span">{student.name}</span>
          <i className={iclassName}>&nbsp;</i>
        </button>
      );
    }
  };

  const getStudent = (student, way) => {
    const link = <span className="glyphicon glyphicon-calendar"></span>;
    const button = getSwipeButton(student, way);
    const calendar_button_class = "btn btn-default calendar-button";
    const calendar_button = (
      <div
        onClick={function () {
          navigate("/students/" + student._id);
        }}
        className={calendar_button_class}
      >
        {link}
      </div>
    );

    if (way !== "out") {
      return (
        <div key={student._id} className="btn-group student-listing col-sm-11" role={"group"}>
          {calendar_button}
          {button}
        </div>
      );
    } else {
      return (
        <div key={student._id} className="btn-group student-listing col-sm-11" role={"group"}>
          {button}
          {calendar_button}
        </div>
      );
    }
  };

  const absentCol = [];
  const notYetInCol = [];
  const inCol = [];
  const outCol = [];

  const sortedStudents = [...students].sort((a, b) => {
    return a.name.toLowerCase() > b.name.toLowerCase() ? 1 : -1;
  });

  sortedStudents.forEach((student) => {
    if (!student.in_today && student.absent_today) {
      absentCol.push(getStudent(student, "absent"));
    } else if (!student.in_today && !student.absent_today) {
      notYetInCol.push(getStudent(student, "notYetIn"));
    } else if (student.in_today && student.last_swipe_type === "in") {
      inCol.push(getStudent(student, "in"));
    } else if (student.in_today && student.last_swipe_type === "out") {
      outCol.push(getStudent(student, "out"));
    }
  });

  return (
    <div className="row">
      <SwipeHelpers
        student={swipeState.student}
        missingDirection={swipeState.missingDirection}
        missingTime={swipeState.missingTime}
        onSwipeComplete={handleSwipeComplete}
        onCancel={handleSwipeCancel}
        onTimeChange={updateMissingTime}
      />
      <div className="row student-listing-table">
        <div className="col-md-3 column">
          <div className="panel panel-info absent">
            <div className="panel-heading absent">
              <b>Not Coming In ({absentCol.length})</b>
            </div>
            <div className="panel-body row">{absentCol}</div>
          </div>
        </div>
        <div className="col-md-3 column not-in">
          <div className="panel panel-info">
            <div className="panel-heading">
              <b>Not Yet In ({notYetInCol.length})</b>
            </div>
            <div className="panel-body row">{notYetInCol}</div>
          </div>
        </div>
        <div className="col-md-3 column in">
          <div className="panel panel-info">
            <div className="panel-heading">
              <b>In ({inCol.length})</b>
            </div>
            <div className="panel-body row">{inCol}</div>
          </div>
        </div>
        <div className="col-md-3 column out">
          <div className="panel panel-info">
            <div className="panel-heading">
              <b>Out ({outCol.length})</b>
            </div>
            <div className="panel-body row">{outCol}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
