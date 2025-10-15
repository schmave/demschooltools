import dayjs from "dayjs";
import React from "react";

import constants from "./appconstants";
import dispatcher from "./appdispatcher";
import Modal from "./modal.jsx";
import * as actionCreator from "./studentactioncreator";
import userStore from "./userstore";

export default class SwipeHelpers extends React.Component {
  state = {
    missing_time: undefined,
    missing_direction: undefined,
    student: undefined,
  };

  _swipeWithMissing = () => {
    const student = this.state.student;
    actionCreator.swipeStudent(student, this.state.missing_direction, this.state.missing_time);
    this.setState({ student: undefined, missing_direction: undefined });
    dispatcher.dispatch({
      type: constants.systemEvents.FLASH,
      message: `Your missing time was recorded. You can now continue to sign ${student.direction}.`,
    });
  };

  validateSignDirection = (student, direction) => {
    this.setState({ student, missing_direction: undefined });
    student.direction = direction;
    if (student.last_swipe_type === direction) {
      const missing_direction = direction === "in" ? "out" : "in";
      this.setState({ missing_direction });
      const school = userStore.getSelectedSchool();
      const defaultOutHours = school._id === 11 ? 16 : 15;
      const hours = missing_direction === "in" ? 9 : defaultOutHours;
      this.setState({ missing_time: `${hours}:00` });
    } else {
      actionCreator.swipeStudent(student, direction);
    }
  };

  componentDidUpdate = () => {
    if (this.state.missing_direction !== undefined) {
      this.refs.missingSwipeCollector.show();
    }
  };

  render() {
    const self = this;
    return (
      this.state.missing_direction !== undefined && (
        <div className="row">
          <Modal
            ref="missingSwipeCollector"
            title={"What time did you sign " + this.state.missing_direction + "?"}
          >
            <form className="form-inline">
              <div className="form-group">
                <div style={{ marginBottom: "2em" }}>
                  You forgot to sign out on{" "}
                  {dayjs(this.state.student.last_swipe_date).format("dddd, MMMM D")}!
                </div>
                <label htmlFor="missing">
                  What time did you sign {this.state.missing_direction}?
                </label>{" "}
                <input
                  type="time"
                  format="hh:mm a"
                  date={false}
                  id="missing"
                  ref="missing_timepicker"
                  step={60 * 15}
                  defaultValue={this.state.missing_time}
                  onChange={function (e) {
                    self.setState({ missing_time: e.target.value });
                  }}
                />
                <div style={{ textAlign: "center" }}>
                  <button
                    id="submit-missing"
                    className="btn btn-sm btn-primary"
                    onClick={this._swipeWithMissing}
                  >
                    Sign {this.state.missing_direction}{" "}
                  </button>
                </div>
              </div>
            </form>
          </Modal>
        </div>
      )
    );
  }
}
