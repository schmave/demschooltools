import React, { useCallback, useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";

import studentStore from "./StudentStore";
import Heatmap from "./heatmap.jsx";
import { useSwipeLogic } from "./hooks/useSwipeLogic";
import StudentEditor from "./student/studentEditor.jsx";
import * as actionCreator from "./studentactioncreator.js";
import SwipeHelpers from "./swipeHelpers.jsx";
import SwipesListing from "./swipeslisting.jsx";
import userStore from "./userstore";

const groupingFunc = function (data) {
  return data.day.split("-")[0] + "-" + data.day.split("-")[1];
};

export default function Student() {
  const { studentId, day } = useParams();

  const [student, setStudent] = useState(() => studentStore.getStudent(studentId, true));
  const [selectedMonth, setSelectedMonth] = useState("");
  const [activeDay, setActiveDay] = useState("");

  const studentEditorRef = useRef();
  const {
    swipeState,
    validateSignDirection,
    handleSwipeComplete,
    handleSwipeCancel,
    updateMissingTime,
  } = useSwipeLogic();

  const getActiveDay = useCallback(
    (student) => {
      if (day) {
        return day;
      } else if (student && student.days[0]) {
        return student.days[0].day;
      } else {
        return "";
      }
    },
    [day],
  );

  useEffect(() => {
    const onChange = () => {
      const s = studentStore.getStudent(studentId);
      const activeDay = getActiveDay(s);
      setStudent(s);
      setSelectedMonth(activeDay.substr(0, 7));
      setActiveDay(activeDay);
    };

    studentStore.addChangeListener(onChange);

    return () => {
      studentStore.removeChangeListener(onChange);
    };
  }, [studentId, getActiveDay]);

  const signIn = () => {
    validateSignDirection(student, "in");
  };

  const signOut = () => {
    validateSignDirection(student, "out");
  };

  const markAbsent = () => {
    actionCreator.markAbsent(student);
  };

  const studentInToday = () => {
    return student.in_today;
  };

  const getActionButtons = () => {
    const buttons = [];

    if (!studentInToday() || student.last_swipe_type === "out") {
      buttons.push(
        <button
          key="sign-in"
          type="button"
          id="sign-in"
          onClick={signIn}
          className="btn btn-sm btn-info margined"
        >
          Sign In
        </button>,
      );
    }
    if (studentInToday() && student.last_swipe_type === "in") {
      buttons.push(
        <button
          key="sign-out"
          type="button"
          id="sign-out"
          onClick={signOut}
          className="btn btn-sm btn-info margined"
        >
          Sign Out
        </button>,
      );
    }
    if (!student.absent_today) {
      buttons.push(
        <button
          key="absent-button"
          id="absent-button"
          type="button"
          onClick={markAbsent}
          className="btn btn-sm btn-info margined"
        >
          Absent today
        </button>,
      );
    }

    return buttons;
  };

  const getDayStatus = (day) => {
    let r = "";
    if (day.valid) {
      r = " ✓";
    }
    if (day.excused) {
      return r + " (Excused)";
    } else if (day.override) {
      return r + " (Overridden)";
    } else {
      return r;
    }
  };

  const getDayClass = (day) => {
    if (day.valid == true) {
      return "attended-day";
    }
    if (day.absent == true) {
      return "absent-day";
    }
    return "";
  };

  const toggleMonth = (month) => {
    if (selectedMonth === month) {
      setSelectedMonth("");
    } else {
      setSelectedMonth(month);
    }
  };

  const openMonth = (month) => {
    setSelectedMonth(month);
  };

  const listMonth = (days, show, month) => {
    return days.map(function (day) {
      const hide = !show ? "hidden" : "";
      const selected = day.day === getActiveDay(student) ? "selected" : "";
      const clsName = hide + " " + selected;
      return (
        <tr key={month + day.day} className={clsName}>
          <td>
            <Link
              to={"/students/" + studentId + "/" + day.day}
              onClick={openMonth.bind(null, month)}
              id={"day-" + day.day}
              className={getDayClass(day)}
            >
              {day.day} {getDayStatus(day)}
            </Link>
          </td>
        </tr>
      );
    });
  };

  const getPreviousDays = () => {
    const selectedDay = day;
    if (!selectedDay && activeDay) {
      // routerc.get().transitionTo('swipes', {studentId :studentId, day: activeDay});
    }
    const groupedDays = student.days.groupBy(groupingFunc);
    const months = Object.keys(groupedDays);
    // This returns a list of lists (of lists?), which React appears to flatten.
    return months.map(function (month) {
      const cls =
        month === selectedMonth
          ? "glyphicon glyphicon-chevron-down"
          : "glyphicon glyphicon-chevron-right";
      return [
        <tr key={month} style={{ fontWeight: "bold" }}>
          <td onClick={toggleMonth.bind(null, month)} style={{ fontWeight: "bold" }}>
            <span className={cls} style={{ paddingRight: "3px" }}></span>
            {month}
          </td>
        </tr>,
        listMonth(groupedDays[month], selectedMonth == month, month),
      ];
    });
  };

  const toggleEdit = () => {
    if (userStore.isAdmin()) {
      studentEditorRef.current.edit(student);
    }
  };

  const showingStudentName = () => {
    return (
      <div className="col-sm-6" id="studentName">
        <span id="edit-name">
          <h1 className="pull-left">{student.name}</h1>
          <span className="fa fa-pencil edit-student"></span>
        </span>

        <h2 className="badge badge-red">
          {!studentInToday() && student.absent_today ? "Absent" : ""}
        </h2>
      </div>
    );
  };

  if (student) {
    const activeDate = getActiveDay(student);
    const requiredMinutes = student.required_minutes;
    return (
      <div className="row">
        <StudentEditor ref={studentEditorRef} />
        <SwipeHelpers
          student={swipeState.student}
          missingDirection={swipeState.missingDirection}
          missingTime={swipeState.missingTime}
          onSwipeComplete={handleSwipeComplete}
          onCancel={handleSwipeCancel}
          onTimeChange={updateMissingTime}
        />
        <div className="col-sm-1"></div>
        <div className="col-sm-10">
          <div className="panel panel-info">
            <div className="panel-heading">
              <div className="row" onClick={toggleEdit}>
                {showingStudentName()}
                <div className="col-sm-6">
                  <div id="hd-attended" className="col-sm-6">
                    <b>Attended:</b> {student.total_days + student.total_short}
                  </div>
                  <div className="col-sm-6">
                    <b>Short:</b> {student.total_short}
                  </div>
                  <div id="hd-absent" className="col-sm-6">
                    <b>Unexcused:</b> {student.total_abs}
                  </div>
                  <div id="hd-excused" className="col-sm-6">
                    <b>Excused:</b> {student.total_excused}
                  </div>
                  <div id="hd-given" className="col-sm-6">
                    <b>Override:</b> {student.total_overrides}
                  </div>
                  <div id="hd-required-mins" className="col-sm-6">
                    <b>Required Minutes:</b> {requiredMinutes}
                  </div>
                </div>
              </div>
            </div>
            <div className="panel-body">
              <div className="row">
                <div className="col-md-7">
                  <div className="row">{getActionButtons()}</div>
                  <Heatmap days={student.days} requiredMinutes={requiredMinutes} />
                </div>
                <div className="col-md-2">
                  <table className="table table-striped center">
                    <thead>
                      <tr>
                        <th className="center">Attendance</th>
                      </tr>
                    </thead>
                    <tbody>{getPreviousDays()}</tbody>
                  </table>
                </div>
                <div className="col-md-3">
                  {activeDate && student ? (
                    <SwipesListing student={student} day={activeDate} />
                  ) : (
                    ""
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
        <div className="col-sm-1"></div>
      </div>
    );
  } else {
    // no student found
    return <div></div>;
  }
}
