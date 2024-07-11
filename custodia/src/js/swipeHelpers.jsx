const React = require("react");
const dayjs = require("dayjs");

const actionCreator = require("./studentactioncreator");
const userStore = require("./userstore");
const Modal = require("./modal.jsx");
const constants = require("./appconstants");
const dispatcher = require("./appdispatcher");

module.exports = class SwipeHelpers extends React.Component {
  state = {
    missing_date: undefined,
    missing_direction: undefined,
    student: undefined,
  };

  _swipeWithMissing = () => {
    const student = this.state.student;
    actionCreator.swipeStudent(student, this.state.missing_direction, this.state.missing_date);
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
      this._setCalendarTime(student, missing_direction);
    } else {
      actionCreator.swipeStudent(student, direction);
    }
  };

  _setCalendarTime = (student, missing_direction) => {
    let d = new Date();
    if (!student) {
      return d;
    }

    if (missing_direction === "out") {
      d = new Date(student.last_swipe_date + "T00:00:00");
    }
    const school = userStore.getSelectedSchool();
    const defaultOutTime = school._id === 11 ? 16 : 15;
    d.setHours(missing_direction === "in" ? 9 : defaultOutTime);
    this.setState({ missing_date: d });
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
                </label>
                <input
                  type="time"
                  format="hh:mm a"
                  date={false}
                  id="missing"
                  ref="missing_datepicker"
                  step={60 * 15}
                  defaultValue={this.state.missing_date}
                  onChange={function (value) {
                    self.setState({ missing_date: value });
                  }}
                />
              </div>
              <div className="form-group" style={{ marginLeft: "2em" }}>
                <button
                  id="submit-missing"
                  className="btn btn-sm btn-primary"
                  onClick={this._swipeWithMissing}
                >
                  Sign {this.state.missing_direction}{" "}
                </button>
              </div>
            </form>
          </Modal>
        </div>
      )
    );
  }
};
