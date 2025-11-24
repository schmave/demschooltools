import { useState } from "react";
import { useNavigate } from "react-router-dom";

import constants from "../appconstants.js";
import dispatcher from "../appdispatcher.js";
import * as actionCreator from "../studentactioncreator.js";
import userStore from "../userstore.js";

export const useSwipeLogic = () => {
  const navigate = useNavigate();
  const [swipeState, setSwipeState] = useState({
    student: null,
    missingDirection: null,
    missingTime: null,
  });

  const validateSignDirection = (student, direction) => {
    setSwipeState((prev) => ({ ...prev, student, missingDirection: null }));
    student.direction = direction;

    if (student.last_swipe_type === direction) {
      const missingDirection = direction === "in" ? "out" : "in";
      const school = userStore.getSelectedSchool();
      const defaultOutHours = school._id === 11 ? 16 : 15;
      const hours = missingDirection === "in" ? 9 : defaultOutHours;
      const missingTime = `${hours}:00`;

      setSwipeState((prev) => ({
        ...prev,
        missingDirection,
        missingTime,
      }));
    } else {
      actionCreator.swipeStudent(student, direction, undefined, navigate);
    }
  };

  const handleSwipeComplete = (missingTime) => {
    const { student, missingDirection } = swipeState;
    if (student && missingDirection) {
      actionCreator.swipeStudent(student, missingDirection, missingTime, navigate);
      setSwipeState({ student: null, missingDirection: null, missingTime: null });
      dispatcher.dispatch({
        type: constants.systemEvents.FLASH,
        message: `Your missing time was recorded. You can now continue to sign ${student.direction}.`,
      });
    }
  };

  const handleSwipeCancel = () => {
    setSwipeState({ student: null, missingDirection: null, missingTime: null });
  };

  const updateMissingTime = (time) => {
    setSwipeState((prev) => ({ ...prev, missingTime: time }));
  };

  return {
    swipeState,
    validateSignDirection,
    handleSwipeComplete,
    handleSwipeCancel,
    updateMissingTime,
  };
};
