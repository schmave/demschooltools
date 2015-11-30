var exports = Object.create(base);

exports.getMissingSwipe: function (student) {
    var missingdirection = (student.last_swipe_type == "in") ? "out" : "in";
    if (!student.in_today
            // && student.last_swipe_type == "in"
        && student.direction == "out") {
        missingdirection = "in";
    }

    this.setState({missingdirection: missingdirection});
    this.refs.missingSwipeCollector.show();
    // use ComponentDidUpdate to check the states then change them
    // use the onChange for the datepicker to mutate setState
}
exports.swipeWithMissing: function (student, missing) {
    var missing = this.refs.missing_swiperef.state.value;
    actionCreator.swipeStudent(student, student.direction, missing);
}
exports.validateSignDirection: function (student,direction) {
    student.direction = direction;
    var missing_in = ((student.last_swipe_type == "out"
        || (student.last_swipe_type == "in" && !student.in_today)
        || !student.last_swipe_type)
        && direction == "out"),
        missing_out = (student.last_swipe_type == "in"
        && direction == "in");

    if (missing_in || missing_out) {
        this.getMissingSwipe();
    } else {
        actionCreator.swipeStudent(student, direction);
    }
}
exports.getMissingTime: function(student) {
    var d = new Date();
    if (student.last_swipe_date) {
        d = new Date(student.last_swipe_date + "T10:00:00");
    }
    if (!student.in_today
        && student.direction == "out") {
        d = new Date();
    }
    d.setHours((this.state.missingdirection == "in") ? 9 : 15);
    d.setMinutes(0);

    if(this.refs.missing_swiperef) {
        this.refs.missing_swiperef.state.value = d;
    }
    return d;
}
module.exports = exports;
