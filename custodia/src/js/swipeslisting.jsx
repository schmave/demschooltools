var React = require('react'),
  PropTypes = require('prop-types'),
  AdminItem = require('./adminwrapper.jsx'),
  actionCreator = require('./studentactioncreator');

class SwipesListing extends React.Component {
    static contextTypes = {
        router: PropTypes.object
    };

    getCurrentDay = (student, dayString) => {
        if(student) {
            var day = student.days.find(function (day) {
                return day.day === dayString;
            }.bind(this));
            return day;
        }else{
            return {};
        }
    };

    componentWillReceiveProps(newProps) {
      this.setState({day: this.getCurrentDay(newProps.student, newProps.day)});
    }

    deleteSwipe = (swipe) => {
        actionCreator.deleteSwipe(swipe, this.props.student);
    };

    swipesAreEmpty = (swipes) => {
        return swipes.length === 0 ||
            (swipes.length === 1 && swipes[0].in_time === null && swipes[0].out_time === null);
    };

    getSwipesForDay = () => {
        var swipeRows = [];
        if (this.state.day && !this.swipesAreEmpty(this.state.day.swipes)) {
            this.state.day.swipes.map(function (swipe) {
                if (swipe.nice_in_time || swipe.nice_out_time) {
                    swipeRows.push(<tr key={swipe._id}>
                        <td>{swipe.nice_in_time}</td>
                        <td>{swipe.nice_out_time}</td>
                        <AdminItem>
                          <td onClick={this.deleteSwipe.bind(this, swipe)}><a>Delete</a></td>
                        </AdminItem>
                    </tr>)
                }
            }.bind(this))
        } else {
            return <tr><td colSpan="2">No swipes available.</td></tr>;
        }
        return swipeRows;
    };

    excuse = (swipe) => {
        actionCreator.excuse(this.props.student._id, this.props.day);
    };

    override = () => {
        actionCreator.override(this.props.student._id, this.props.day);
    };

    state = {day: this.getCurrentDay(this.props.student, this.props.day)};

    render() {
        this.state.day.round_mins = parseFloat(this.state.day.total_mins).toFixed(0);
        var showOverrideExcuseButtons = this.state.day && !this.state.day.override
                                 && !this.state.day.excused && !this.state.day.valid;
        return <span>
                        <div>
                            Day: {this.state.day.day}
                        </div>
                        <div>
                            Minutes: {this.state.day.round_mins}
                        </div>
            <table className="table table-striped center">
                <thead>
                <tr>
                    <th className="center">In Time</th>
                    <th className="center">Out Time</th>
                </tr>
                </thead>
                <tbody>
                {this.getSwipesForDay()}
                </tbody>
            </table>
            {showOverrideExcuseButtons ?
              <AdminItem>
                <div className="action-buttons">
                  <div className="pull-left">
                    <button type="button" id="override" onClick={this.override} className="btn btn-sm btn-info">
                        Override
                    </button>
                  </div>
                  <div className="pull-right">
                    <button type="button" onClick={this.excuse} className="btn btn-sm btn-info">Excuse</button>
                  </div>
                </div>
              </AdminItem>
              : ''
            }
            </span>;
    }
}

module.exports = SwipesListing;
