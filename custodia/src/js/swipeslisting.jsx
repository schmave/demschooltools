const React = require("react");
const PropTypes = require("prop-types");
const AdminItem = require("./adminwrapper.jsx");
const actionCreator = require("./studentactioncreator");

class SwipesListing extends React.Component {
  static contextTypes = {
    router: PropTypes.object,
  };

  getCurrentDay = (student, dayString) => {
    if (student) {
      const day = student.days.find(function (day) {
        return day.day === dayString;
      });
      return day;
    } else {
      return {};
    }
  };

  componentWillReceiveProps(newProps) {
    this.setState({ day: this.getCurrentDay(newProps.student, newProps.day) });
  }

  deleteSwipe = (swipe) => {
    actionCreator.deleteSwipe(swipe, this.props.student);
  };

  swipesAreEmpty = (swipes) => {
    return (
      swipes.length === 0 ||
      (swipes.length === 1 && swipes[0].in_time === null && swipes[0].out_time === null)
    );
  };

  getSwipesForDay = () => {
    const swipeRows = [];
    if (this.state.day && !this.swipesAreEmpty(this.state.day.swipes)) {
      this.state.day.swipes.map(
        function (swipe) {
          if (swipe.nice_in_time || swipe.nice_out_time) {
            swipeRows.push(
              <tr key={swipe._id}>
                <td>{swipe.nice_in_time}</td>
                <td>{swipe.nice_out_time}</td>
                <AdminItem>
                  <td onClick={this.deleteSwipe.bind(this, swipe)}>
                    <a>Delete</a>
                  </td>
                </AdminItem>
              </tr>,
            );
          }
        }.bind(this),
      );
    } else {
      return (
        <tr>
          <td colSpan="2">No swipes available.</td>
        </tr>
      );
    }
    return swipeRows;
  };

  excuse = (swipe) => {
    actionCreator.excuse(this.props.student._id, this.props.day);
  };

  override = () => {
    actionCreator.override(this.props.student._id, this.props.day);
  };

  state = { day: this.getCurrentDay(this.props.student, this.props.day) };

  render() {
    if (!this.state.day) {
      return null;
    }
    this.state.day.round_mins = parseFloat(this.state.day.total_mins).toFixed(0);
    const showOverrideExcuseButtons =
      this.state.day &&
      !this.state.day.override &&
      !this.state.day.excused &&
      !this.state.day.valid;
    return (
      <span>
        <div>Day: {this.state.day.day}</div>
        <div>Minutes: {this.state.day.round_mins}</div>
        <table className="table table-striped center swipes-table">
          <thead>
            <tr>
              <th className="center">In Time</th>
              <th className="center">Out Time</th>
            </tr>
          </thead>
          <tbody>{this.getSwipesForDay()}</tbody>
        </table>
        {showOverrideExcuseButtons ? (
          <AdminItem>
            <div className="action-buttons">
              <button
                type="button"
                id="override"
                onClick={this.override}
                className="btn btn-sm btn-info"
              >
                Override {this.state.day.day}
              </button>
              <button type="button" onClick={this.excuse} className="btn btn-sm btn-info">
                Excuse {this.state.day.day}
              </button>
            </div>
          </AdminItem>
        ) : (
          ""
        )}
      </span>
    );
  }
}

module.exports = SwipesListing;
