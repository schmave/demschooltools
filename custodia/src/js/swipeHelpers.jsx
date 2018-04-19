var React = require('react'),
    Router = require('react-router'),
    AdminItem = require('./adminwrapper.jsx'),
    DateTimePicker = require('react-widgets').DateTimePicker,
    actionCreator = require('./studentactioncreator'),
    Modal = require('./modal.jsx');

module.exports = class extends React.Component {
    static displayName = "SwipeHelpers";

    state = {
        missing_date: new Date(),
    };

    _getMissingSwipe = (student) => {
        var missingdirection = (student.last_swipe_type == "in") ? "out" : "in";
        if (!student.in_today && student.direction == "out") {
            missingdirection = "in";
        }
        this.setState({missingdirection: missingdirection});
        this.refs.missingSwipeCollector.show();
        return missingdirection;
        // use ComponentDidUpdate to check the states then change them
        // use the onChange for the datepicker to mutate setState
    };

    _swipeWithMissing = (missing) => {
        var student = this.state.student,
            missing = this.state.missing_date;
        actionCreator.swipeStudent(student, student.direction, missing);
        this.setState({student: {}, missingdirection: false})
        this.refs.missingSwipeCollector.hide();
    };

    validateSignDirection = (student, direction) => {
        this.setState({student: student})
        student.direction = direction;
        var missing_in = ((student.last_swipe_type == "out"
                        || (student.last_swipe_type == "in" && !student.in_today)
                        || !student.last_swipe_type)
                       && direction == "out"),
            missing_out = (student.last_swipe_type == "in"
                        && direction == "in");

        if ((missing_in || missing_out) && student.last_swipe_date) {
            var missingD = this._getMissingSwipe(student);
            this._setCalendarTime(student, missingD);
        } else {
            actionCreator.swipeStudent(student, direction);
        }
    };

    _setCalendarTime = (student, missingdirection) => {
        var d = new Date();
        if (!student) { return d;}
        if (student.last_swipe_date) {
            var lastDate = student.last_swipe_date.replace(/T.*/, "");
            d = new Date(lastDate + "T10:00:00");
        }
        if (!student.in_today
            && student.direction == "out") {
            d = new Date();
        }
        d.setHours((missingdirection == "in") ? 9 : 15);
        d.setMinutes(0);


        // TODO(Evan): This code is supposed to update the value of the time picker
        //             but it doesn't.
        // if(this.refs.missing_datepicker) {
        //    this.refs.missing_datepicker.state.value = d;
        //}
        return d;
    };

    render() {
        var self = this;
        return <div className="row">
          <Modal ref="missingSwipeCollector"
                 title={"What time did you sign " + this.state.missingdirection + "?"}>
            <form className="form-inline">
              <div className="form-group">
                <label htmlFor="missing">What time did you sign {this.state.missingdirection}?</label>
                <DateTimePicker format="hh:mm a"
                                id="missing" defaultValue={new Date()}
                                ref="missing_datepicker"
                                calendar={false}
                                onChange={function(value) {
                                    console.log(value);
                                    self.setState({missing_date: value});
                                }}/>
              </div>
              <div className="form-group" style={{marginLeft: '2em'}}>
                <button id="submit-missing" className="btn btn-sm btn-primary" onClick={this._swipeWithMissing}>
                  Sign {this.state.missingdirection} </button>
              </div>
            </form>
          </Modal></div>;
    }

    _onChange = () => {
        if (this.refs.missingSwipeCollector) {
            this.refs.missingSwipeCollector.hide();
        }
    };
};
