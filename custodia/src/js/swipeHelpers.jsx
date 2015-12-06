var React = require('react'),
    Router = require('react-router'),
    AdminItem = require('./adminwrapper.jsx'),
    DateTimePicker = require('react-widgets').DateTimePicker,
    actionCreator = require('./studentactioncreator'),
    Modal = require('./modal.jsx');

module.exports = React.createClass({
    getInitialState: function() {
        return {};
    },
    _getMissingSwipe : function(student) {
        var missingdirection = (student.last_swipe_type == "in") ? "out" : "in";
        if (!student.in_today && student.direction == "out") {
            missingdirection = "in";
        }
        this.setState({missingdirection: missingdirection});
        this.refs.missingSwipeCollector.show();
        return missingdirection;
        // use ComponentDidUpdate to check the states then change them
        // use the onChange for the datepicker to mutate setState
    },

    _swipeWithMissing: function(missing) {
        var student = this.state.student,
            missing = this.refs.missing_datepicker.state.value;
        actionCreator.swipeStudent(student, student.direction, missing);
        this.setState({student: {}, missingdirection: false})
        this.refs.missingSwipeCollector.hide();
    },

    validateSignDirection: function(student, direction) {
        this.setState({student: student})
        student.direction = direction;
        var missing_in = ((student.last_swipe_type == "out"
                           || (student.last_swipe_type == "in" && !student.in_today)
                           || !student.last_swipe_type)
                          && direction == "out"),
            missing_out = (student.last_swipe_type == "in"
                           && direction == "in");

        if (missing_in || missing_out) {
            var missingD = this._getMissingSwipe(student);
            this._setCalendarTime(student, missingD);
        } else {
            actionCreator.swipeStudent(student, direction);
        }
    },
    _setCalendarTime: function(student, missingdirection) {
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

        if(this.refs.missing_datepicker) {
            this.refs.missing_datepicker.state.value = d;
        }
        return d;
    },
    render: function()  {
        return <div className="row">
            <Modal ref="missingSwipeCollector"
                       title={"What time did you sign " + this.state.missingdirection + "?"}>
                    <form className="form-inline">
                        <div className="form-group">
                            <label htmlFor="missing">What time did you sign {this.state.missingdirection}?</label>
            <DateTimePicker id="missing" defaultValue={new Date()}
                                         ref="missing_datepicker"
                                         calendar={false}/>
                        </div>
                        <div className="form-group" style={{marginLeft: '2em'}}>
                <button id="submit-missing" className="btn btn-sm btn-primary" onClick={this._swipeWithMissing}>
                                Sign {this.state.missingdirection} </button>
                        </div>
                    </form>
            </Modal></div>;
    },

    _onChange: function() {
        if (this.refs.missingSwipeCollector) {
            this.refs.missingSwipeCollector.hide();
        }
    }
});
