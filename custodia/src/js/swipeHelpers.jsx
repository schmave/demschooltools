var React = require('react'),
    Router = require('react-router'),
    AdminItem = require('./adminwrapper.jsx'),
    DateTimePicker = require('react-widgets').DateTimePicker,
    actionCreator = require('./studentactioncreator'),
    Modal = require('./modal.jsx');

module.exports = class extends React.Component {
    static displayName = "SwipeHelpers";

    state = {
        missing_date: undefined,
        missing_direction: undefined,
        student: undefined,
    };

    _swipeWithMissing = (missing) => {
        var student = this.state.student,
            missing = this.state.missing_date;
        actionCreator.swipeStudent(student, student.direction, missing);
        this.setState({student: undefined, missing_direction: undefined})
    };

    validateSignDirection = (student, direction) => {
        this.setState({student: student, missing_direction: undefined})
        student.direction = direction;
        if (student.last_swipe_type === direction) {
            var missing_direction = direction === 'in' ? 'out' : 'in';
            this.setState({missing_direction: missing_direction});
            this._setCalendarTime(student, missing_direction);
        } else {
            actionCreator.swipeStudent(student, direction);
        }
    };

    _setCalendarTime = (student, missing_direction) => {
        var d = new Date();
        if (!student) { return d;}

        if (missing_direction === 'out') {
            d = new Date(student.last_swipe_date + 'T00:00:00');
        }
        d.setHours(missing_direction === "in" ? 9 : 15);
        console.log('suggesting', d);
        this.setState({missing_date: d});
    };

    componentDidUpdate = () => {
        if (this.state.missing_direction !== undefined) {
            this.refs.missingSwipeCollector.show();
        }
    }

    render() {
        var self = this;
        if (this.state.missing_direction !== undefined) {
            console.log('render', this.state.missing_date);
        }
        return this.state.missing_direction === undefined ? '' : <div className="row">
          <Modal ref="missingSwipeCollector"
                 title={"What time did you sign " + this.state.missing_direction + "?"}>
            <form className="form-inline">
              <div className="form-group">
                <label htmlFor="missing">What time did you sign {this.state.missing_direction}?</label>
                <DateTimePicker format="MMM dd, yyyy hh:mm a"
                                date={true}
                                id="missing"
                                ref="missing_datepicker"
                                defaultOpen="time"
                                step={15}
                                defaultValue={this.state.missing_date}
                                onChange={function(value) {
                                    console.log(value);
                                    self.setState({missing_date: value});
                                }}/>
              </div>
              <div className="form-group" style={{marginLeft: '2em'}}>
                <button id="submit-missing" className="btn btn-sm btn-primary" onClick={this._swipeWithMissing}>
                  Sign {this.state.missing_direction} </button>
              </div>
            </form>
          </Modal></div>;
    }
};
