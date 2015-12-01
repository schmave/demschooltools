var actionCreator = require('./studentactioncreator'),
    base = require('./storebase'),
    exports = Object.create(base);

var getMissingSwipe = function (that) {
    var missingdirection = (that.state.student.last_swipe_type == "in") ? "out" : "in";
    if (!that.state.student.in_today
            // && that.state.student.last_swipe_type == "in"
        && that.state.student.direction == "out") {
        missingdirection = "in";
    }

    that.setState({missingdirection: missingdirection});
    that.refs.missingSwipeCollector.show();
    // use ComponentDidUpdate to check the states then change them
    // use the onChange for the datepicker to mutate setState
};

exports.swipeWithMissing = function (that, missing) {
    var student = that.state.student,
        missing = that.refs.missing_swiperef.state.value;
    actionCreator.swipeStudent(student, student.direction, missing);
};

exports.validateSignDirection = function (that, direction) {
    that.state.student.direction = direction;
    var missing_in = ((that.state.student.last_swipe_type == "out"
        || (that.state.student.last_swipe_type == "in" && !that.state.student.in_today)
        || !that.state.student.last_swipe_type)
        && direction == "out"),
        missing_out = (that.state.student.last_swipe_type == "in"
        && direction == "in");

    if (missing_in || missing_out) {
        getMissingSwipe(that);
    } else {
        actionCreator.swipeStudent(that.state.student, direction);
    }
};

exports.getMissingTime = function(that) {
    var d = new Date();
    if (that.state.student.last_swipe_date) {
        d = new Date(that.state.student.last_swipe_date + "T10:00:00");
    }
    if (!that.state.student.in_today
        && that.state.student.direction == "out") {
        d = new Date();
    }
    d.setHours((that.state.missingdirection == "in") ? 9 : 15);
    d.setMinutes(0);

    if(that.refs.missing_swiperef) {
        that.refs.missing_swiperef.state.value = d;
    }
    return d;
};
module.exports = exports;
