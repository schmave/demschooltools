var React = require('react'),
    store = require('./StudentStore'),
    actionCreator = require('./studentactioncreator'),
    studentStore = require('./StudentStore');

var exports = React.createClass({
    contextTypes: {
        router: React.PropTypes.func
    },
    getCurrentDay: function (student, dayString) {
        if(student) {
            var day = student.days.find(function (day) {
                return day.day === dayString;
            }.bind(this));
            return day;
        }else{
            return {};
        }
    },
    componentWillReceiveProps: function(newProps){
      this.setState({day: this.getCurrentDay(newProps.student, newProps.day)});
    },
    getInitialState: function(){
        return {day: this.getCurrentDay(this.props.student, this.props.day)};
    },
    deleteSwipe: function (swipe) {
        actionCreator.deleteSwipe(swipe, this.props.student);
    },
    swipesAreEmpty: function(swipes){
        return swipes.length === 0 ||
            (swipes.length === 1 && swipes[0].in_time === null && swipes[0].out_time === null);
    },
    getSwipesForDay: function () {
        var swipeRows = [];
        if (this.state.day && !this.swipesAreEmpty(this.state.day.swipes)) {
            this.state.day.swipes.map(function (swipe) {
                if (swipe.nice_in_time || swipe.nice_out_time) {
                    swipeRows.push(<tr>
                        <td>{swipe.nice_in_time}</td>
                        <td>{swipe.nice_out_time}</td>
                        <td onClick={this.deleteSwipe.bind(this, swipe)}><a>Delete</a></td>
                    </tr>)
                }
            }.bind(this))
        }else{
            return <td colSpan="2">No swipes available.</td>;
        }
        return swipeRows;
    },
    excuse: function (swipe) {
        actionCreator.excuse(this.props.student._id, this.props.day);
    },
    override: function () {
        actionCreator.override(this.props.student._id, this.props.day);
    },
    render: function () {

        return <span>
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
            {this.state.day && !this.state.day.override && !this.state.day.excused ? <div className="action-buttons">
                <div className="pull-left">
                    <button type="button" onClick={this.override} className="btn btn-sm btn-info">
                        Give Attendence
                    </button>
                </div>
                <div className="pull-right">
                    <button type="button" onClick={this.excuse} className="btn btn-sm btn-info">Excuse</button>
                </div>
            </div> : ''}
        </span>;
    },


});

module.exports = exports;