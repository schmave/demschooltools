const React = require("react");
const AdminItem = require("./adminwrapper.jsx");
const actionCreator = require("./studentactioncreator");

class SwipesListing extends React.Component {
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

  deleteSwipe = (swipe) => {
    actionCreator.deleteSwipe(swipe, this.props.student);
  };

  getSwipesForDay = () => {
    const currentDay = this.getCurrentDay(this.props.student, this.props.day);

    const swipeRows = [];
    if (currentDay?.swipes?.length) {
      currentDay.swipes.map(
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

  onExcuse = (e) => {
    actionCreator.excuse(this.props.student._id, this.props.day, !e.target.checked);
  };

  onOverride = (e) => {
    actionCreator.override(this.props.student._id, this.props.day, !e.target.checked);
  };

  render() {
    const currentDay = this.getCurrentDay(this.props.student, this.props.day);

    if (!currentDay) {
      return null;
    }
    currentDay.round_mins = parseFloat(currentDay.total_mins).toFixed(0);
    return (
      <span>
        <div>Day: {currentDay.day}</div>
        <div>Minutes: {currentDay.round_mins}</div>
        <table className="table table-striped center swipes-table">
          <thead>
            <tr>
              <th className="center">In Time</th>
              <th className="center">Out Time</th>
            </tr>
          </thead>
          <tbody>{this.getSwipesForDay()}</tbody>
        </table>
        {!(currentDay.valid && !currentDay.override && !currentDay.excused) && (
          <AdminItem>
            <div className="action-buttons">
              <label htmlFor="override">
                <input
                  id="override"
                  type="checkbox"
                  onChange={this.onOverride}
                  checked={currentDay.override}
                  disabled={currentDay.excused}
                />{" "}
                Override {currentDay.day}
              </label>
              <label htmlFor="excuse">
                <input
                  id="excuse"
                  type="checkbox"
                  onChange={this.onExcuse}
                  checked={currentDay.excused}
                  disabled={currentDay.override}
                />{" "}
                Excuse {currentDay.day}
              </label>
            </div>
          </AdminItem>
        )}
      </span>
    );
  }
}

module.exports = SwipesListing;
