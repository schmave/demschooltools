import dayjs from "dayjs";
import React from "react";

import Modal from "./modal.jsx";

export default function SwipeHelpers({
  student,
  missingDirection,
  missingTime,
  onSwipeComplete,
  onCancel,
  onTimeChange,
}) {
  const handleSubmit = (e) => {
    e.preventDefault();
    if (onSwipeComplete && missingTime) {
      onSwipeComplete(missingTime);
    }
  };

  const handleTimeChange = (e) => {
    if (onTimeChange) {
      onTimeChange(e.target.value);
    }
  };

  const handleClose = () => {
    if (onCancel) {
      onCancel();
    }
  };

  const isOpen = student && missingDirection;

  if (!isOpen) {
    return null;
  }

  return (
    <div className="row">
      <Modal
        open={true}
        onClose={handleClose}
        title={"What time did you sign " + missingDirection + "?"}
      >
        <form className="form-inline" onSubmit={handleSubmit}>
          <div className="form-group">
            <div style={{ marginBottom: "2em" }}>
              You forgot to sign out on {dayjs(student.last_swipe_date).format("dddd, MMMM D")}!
            </div>
            <label htmlFor="missing">What time did you sign {missingDirection}?</label>{" "}
            <input
              type="time"
              format="hh:mm a"
              date={false}
              id="missing"
              step={60 * 15}
              value={missingTime || ""}
              onChange={handleTimeChange}
            />
            <div style={{ textAlign: "center" }}>
              <button id="submit-missing" className="btn btn-sm btn-primary" type="submit">
                Sign {missingDirection}{" "}
              </button>
            </div>
          </div>
        </form>
      </Modal>
    </div>
  );
}
