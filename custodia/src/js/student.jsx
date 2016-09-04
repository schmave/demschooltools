var React = require('react'),
    Heatmap = require('./heatmap.jsx'),
    userStore = require('./userstore'),
    AdminItem = require('./adminwrapper.jsx'),
    Modal = require('./modal.jsx'),
    DateTimePicker = require('react-widgets').DateTimePicker,
    actionCreator = require('./studentactioncreator'),
    studentStore = require('./StudentStore'),
    Router = require('react-router'),
    Link = Router.Link,
    SwipeHelpers = require('./swipeHelpers.jsx'),
    Swipes = require('./swipeslisting.jsx'),
    SwipeListing = require('./swipeslisting.jsx');

var groupingFunc = function (data) {
    return data.day.split('-')[0] + '-' + data.day.split('-')[1];
};

var exports = React.createClass({
    contextTypes: {
        router: React.PropTypes.func
    },
    getInitialState: function () {
        var studentId = this.context.router.getCurrentParams().studentId;
        return {
            studentId: studentId,
            editing: false,
            student: studentStore.getStudent(studentId)
        };
    },
    componentDidMount: function () {
        studentStore.addChangeListener(this._onChange);
    },
    componentWillUnmount: function () {
        studentStore.removeChangeListener(this._onChange);
    },
    signIn: function () {
        this.refs.missingSwipeCollector.validateSignDirection(this.state.student, 'in');
    },
    signOut: function () {
        this.refs.missingSwipeCollector.validateSignDirection(this.state.student, 'out');
    },
    markAbsent: function () {
        actionCreator.markAbsent(this.state.student);
    },
    studentInToday: function () {
        return this.state.student.last_swipe_date == this.state.student.today;
    },
    getActionButtons: function () {
        var buttons = [];

        if (!this.studentInToday() || this.state.student.last_swipe_type === 'out') {
            buttons.push(<button type="button" id="sign-in" onClick={this.signIn}
                                 className="btn btn-sm btn-info margined">Sign In
            </button>);
        }
        if (!this.studentInToday() || this.state.student.last_swipe_type === 'in') {
            buttons.push(<button type="button" id="sign-out" onClick={this.signOut}
                                 className="btn btn-sm btn-info margined">Sign Out
            </button>);
        }
        if (!this.state.student.absent_today) {
            buttons.push(<button id="absent-button" type="button" onClick={this.markAbsent}
                                 className="btn btn-sm btn-info margined">Absent
            </button>);
        }

        return buttons;
    },
    getDayStatus: function (day) {
        var r = "";
        if (day.valid) {
            r = " ✓";
        }
        if (day.excused) {
            return r + ' (Excused)';
        } else if (day.override) {
            return r + ' (Overridden)';
        } else {
            return r;
        }
    },
    getDayClass: function (day) {
        if (day.valid==true) {
            return "attended-day";
        }
        if (day.absent==true) {
            return "absent-day";
        }
        return "";
    },
    toggleMonth: function(month) {
        if (this.state.selectedMonth === month) {
            this.setState({selectedMonth: "" });
        } else {
            this.setState({selectedMonth: month });
        }
    },
    openMonth: function(month) {
        this.setState({selectedMonth: month });
    },
    listMonth: function(days, show, month) {
        return days.map(function (day, i) {
            var hide = (!show) ? "hidden" : "";
            var selected = day.day === this.getActiveDay(this.state.student) ? "selected" : "";
            var clsName = hide + " " + selected;
            return <tr className={clsName}>
                <td>
                    <Link to="student"
                          onClick={this.openMonth.bind(this, month)}
                          id={"day-"+day.day}
                          className={this.getDayClass(day)}
                          params={{studentId: this.state.studentId, day: day.day}}>
                        {day.day} {this.getDayStatus(day)}
                    </Link>
                </td>
            </tr>;
        }.bind(this));
    },
    getPreviousDays: function () {
        var selectedDay = this.context.router.getCurrentParams().day;
        if (!selectedDay && this.state.day) {
            //routerc.get().transitionTo('swipes', {studentId :this.state.studentId, day: this.state.day});
        }
        var groupedDays = this.state.student.days.groupBy(groupingFunc);
        var months = Object.keys(groupedDays);
        return months.map(function(month){
            var cls = (month===this.state.selectedMonth)
                ? "glyphicon glyphicon-chevron-down"
                    : "glyphicon glyphicon-chevron-right";
            return <span>
            <tr style={{fontWeight:"bold"}}>
                <td onClick={this.toggleMonth.bind(this, month)}
                    style={{fontWeight:"bold"}}>
                    <span className={cls}
                          style={{"padding-right":"3px"}} ></span>
                    {month}
                </td>
            </tr>
            {this.listMonth(groupedDays[month], (this.state.selectedMonth == month), month)}
            </span>;
        }.bind(this));
    },
    toggleEdit: function () {
        if(userStore.isAdmin()) {
            this.setState({editing: !this.state.editing});
        }
    },
    saveChange: function () {
        actionCreator.updateStudent(this.state.student._id,
                                    this.refs.name.getDOMNode().value,
                                    this.refs.missing_datepicker.state.value,
                                    this.refs.email.getDOMNode().value);
        this.toggleEdit();
    },
    getActiveDay: function (student) {
        if (this.context.router.getCurrentParams().day) {
            return this.context.router.getCurrentParams().day;
        } else if (student && student.days[0]) {
            return student.days[0].day;
        } else {
            return '';
        }
    },
    toggleHours: function () {
        this.state.student.olderdate = !!!this.state.student.olderdate;
        actionCreator.toggleHours(this.state.student._id);
    },
    showingStudentName: function () {
        return <div className="col-sm-8" id="studentName">
                <span id="edit-name" onClick={this.toggleEdit}>
                    <h1 className="pull-left">{this.state.student.name}</h1>
                    <span className="fa fa-pencil edit-student"></span>
                </span>

                <h2 className="badge badge-red">{(!this.studentInToday() && this.state.student.absent_today) ? 'Absent' : ''}</h2>
        </div>;
    },
    editingStudentName: function () {
        var pickerDate = (this.state.student.start_date) ? new Date(this.state.student.start_date) : null;
        return <div className="col-sm-8 row">
            <div className="col-sm-3" id="nameRow">
                Name: <input ref="name" className="form-control" id="studentName"
                       defaultValue={this.state.student.name}/>
                Parent Email: <input ref="email" className="form-control" id="email"
                       defaultValue={this.state.student.guardian_email}/>
                <button onClick={this.saveChange} className="btn btn-success">
                    <i id="save-name" className="fa fa-check icon-large">Save</i></button>
                <button id="cancel-name" onClick={this.toggleEdit} className="btn btn-danger">
                    <i className="fa fa-times"></i></button>
            </div>
            <div className="col-md-4" >
                <div><input type="radio" name="older" onChange={this.toggleHours}
                            checked={!this.state.student.olderdate}/> 300 Minutes
                </div>
                <div><input type="radio" name="older" onChange={this.toggleHours}
                            checked={this.state.student.olderdate}/> 330 Minutes
                </div>
            </div>
            <div className="col-md-4" id="nameRow">
                <b>Student Start Date:</b>
                <DateTimePicker id="missing" defaultValue={pickerDate}
                                ref="missing_datepicker"
                                calendar={true}
                                time={false} />
            </div>
        </div>;
    },
    render: function () {
        if (this.state.student) {
            var activeDate = this.getActiveDay(this.state.student);
            var attended = (this.state.student.total_days + this.state.student.total_short).toString()
                + " (" + this.state.student.total_short +  ")",
                requiredMinutes = this.state.student.olderdate ? 330 : 300;
            return <div className="row">
            <SwipeHelpers ref="missingSwipeCollector">
            </SwipeHelpers>

            <div className="col-sm-1"></div>
            <div className="col-sm-10">
                <div className="panel panel-info">
                    <div className="panel-heading">
                        <div className="row">
                            {!this.state.editing ? this.showingStudentName() : this.editingStudentName()}
                            <div className="col-sm-4">
                                <div id="hd-attended" className="col-sm-6"><b>Attended:</b> {attended}</div>
                                <div id="hd-absent" className="col-sm-6"><b>Unexcused:</b> {this.state.student.total_abs}</div>
                                <div id="hd-excused" className="col-sm-6"><b>Excused:</b> {this.state.student.total_excused}</div>
                                <div id="hd-given" className="col-sm-6"><b>Override:</b> {this.state.student.total_overrides}
                                </div>
                                <div id="hd-required-mins" className="col-sm-6"><b>Required
                                    Minutes:</b> {requiredMinutes}</div>
                            </div>
                        </div>
                    </div>
                    <div className="panel-body">
                        <div className="row">
                            <div className="col-sm-7">
                                <div className="row">
                                    {this.getActionButtons()}
                                </div>
                                <Heatmap days={this.state.student.days}
                                         requiredMinutes={requiredMinutes} />
                            </div>
                            <div className="col-sm-2">
                                <table className="table table-striped center">
                                    <thead>
                                        <tr>
                                            <th className="center">Attendance</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {this.getPreviousDays()}
                                    </tbody>
                                </table>
                            </div>
                            <div className="col-sm-2">
                                {activeDate && this.state.student
                                 ? <Swipes student={this.state.student} day={activeDate}/>
                                 : ''}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div className="col-sm-1"></div>
            </div>;

        } else {
            // no student found
            return <div></div>;
        }

    },
    _onChange: function () {
        var s = studentStore.getStudent(this.state.studentId);

        var activeDay = this.getActiveDay(s);
        this.setState({
            student: s,
            selectedMonth : "2016-04",
            activeDay: activeDay
        });
    }
});

module.exports = exports;
