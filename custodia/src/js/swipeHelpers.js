var actionCreator = require('./studentactioncreator'),
    React = require('react'),
    Modal = require('./modal.jsx');

var exports = React.createClass({
    getInitialState: () => {
        return {
            studentId: this.context.router.getCurrentParams().studentId,
            editing: false
        };
    },
    getMissingSwipe : () => {
        var missingdirection = (this.state.student.last_swipe_type == "in") ? "out" : "in";
        if (!this.state.student.in_today
            // && this.state.student.last_swipe_type == "in"
            && this.state.student.direction == "out") {
            missingdirection = "in";
        }
        this.setState({missingdirection: missingdirection});
        this.refs.missingSwipeCollector.show();
        // use ComponentDidUpdate to check the states then change them
        // use the onChange for the datepicker to mutate setState
    },

    swipeWithMissing: (missing) => {
        var student = this.state.student,
            missing = this.refs.missing_swiperef.state.value;
        actionCreator.swipeStudent(student, student.direction, missing);
    },

    validateSignDirection: (direction) => {
        this.state.student.direction = direction;
        var missing_in = ((this.state.student.last_swipe_type == "out"
                           || (this.state.student.last_swipe_type == "in" && !this.state.student.in_today)
                           || !this.state.student.last_swipe_type)
                          && direction == "out"),
            missing_out = (this.state.student.last_swipe_type == "in"
                           && direction == "in");
        if (missing_in || missing_out) {
            getMissingSwipe(this);
        } else {
            actionCreator.swipeStudent(this.state.student, direction);
        }
    },
    getMissingTime: () => {
        var d = new Date();
        if (this.state.student.last_swipe_date) {
            d = new Date(this.state.student.last_swipe_date + "T10:00:00");
        }
        if (!this.state.student.in_today
            && this.state.student.direction == "out") {
            d = new Date();
        }
        d.setHours((this.state.missingdirection == "in") ? 9 : 15);
        d.setMinutes(0);

        if(this.refs.missing_swiperef) {
            this.refs.missing_swiperef.state.value = d;
        }
        return d;
    },

    render: () => {
        return
                <Modal ref="missingSwipeCollector"
                       title={"What time did you sign " + this.state.missingdirection + "?"}>
                    <form className="form-inline">
                        <div className="form-group">
                            <label htmlFor="missing">What time did you sign {this.state.missingdirection}?</label>
                <DateTimePicker id="missing" defaultValue={this.getMissingTime} ref="missing_swiperef"
                                            calendar={false}/>
                        </div>
                        <div className="form-group" style={{marginLeft: '2em'}}>
                <button id="submit-missing" className="btn btn-sm btn-primary" onClick={this.swipeWithMissing}>
                                Sign {this.state.missingdirection} </button>
                        </div>
                    </form>
                </Modal>
    }
});
module.exports = exports;
