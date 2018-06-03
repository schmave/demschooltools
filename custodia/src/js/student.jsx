var React = require('react'),
    Heatmap = require('./heatmap.jsx'),
    userStore = require('./userstore'),
    AdminItem = require('./adminwrapper.jsx'),
    Modal = require('./modal.jsx'),
    actionCreator = require('./studentactioncreator'),
    studentStore = require('./StudentStore'),
    Router = require('react-router'),
    Link = Router.Link,
    SwipeHelpers = require('./swipeHelpers.jsx'),
    StudentEditor = require('./student/studentEditor.jsx'),
    Swipes = require('./swipeslisting.jsx'),
    SwipeListing = require('./swipeslisting.jsx');

var groupingFunc = function (data) {
    return data.day.split('-')[0] + '-' + data.day.split('-')[1];
};

class Student extends React.Component {
    constructor(props) {
        super(props);
        var studentId = props.params.studentId;

        this.state = {
            studentId: studentId,
            student: studentStore.getStudent(studentId)
        };
    }

    componentDidMount() {
        studentStore.addChangeListener(this._onChange);
    }

    componentWillUnmount() {
        studentStore.removeChangeListener(this._onChange);
    }

    signIn = () => {
        this.refs.missingSwipeCollector.validateSignDirection(this.state.student, 'in');
    };

    signOut = () => {
        this.refs.missingSwipeCollector.validateSignDirection(this.state.student, 'out');
    };

    markAbsent = () => {
        actionCreator.markAbsent(this.state.student);
    };

    studentInToday = () => {
        return this.state.student.in_today;
    };

    getActionButtons = () => {
        var buttons = [];

        if (!this.studentInToday() || this.state.student.last_swipe_type === 'out') {
            buttons.push(<button key="sign-in" type="button" id="sign-in" onClick={this.signIn}
                                 className="btn btn-sm btn-info margined">Sign In
            </button>);
        }
        if (this.studentInToday() && this.state.student.last_swipe_type === 'in') {
            buttons.push(<button key="sign-out" type="button" id="sign-out" onClick={this.signOut}
                                 className="btn btn-sm btn-info margined">Sign Out
            </button>);
        }
        if (!this.state.student.absent_today) {
            buttons.push(<button key="absent-button" id="absent-button" type="button" onClick={this.markAbsent}
                                 className="btn btn-sm btn-info margined">Absent
            </button>);
        }

        return buttons;
    };

    getDayStatus = (day) => {
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
    };

    getDayClass = (day) => {
        if (day.valid==true) {
            return "attended-day";
        }
        if (day.absent==true) {
            return "absent-day";
        }
        return "";
    };

    toggleMonth = (month) => {
        if (this.state.selectedMonth === month) {
            this.setState({selectedMonth: "" });
        } else {
            this.setState({selectedMonth: month });
        }
    };

    openMonth = (month) => {
        this.setState({selectedMonth: month });
    };

    listMonth = (days, show, month) => {
        return days.map(function (day, i) {
            var hide = (!show) ? "hidden" : "";
            var selected = day.day === this.getActiveDay(this.state.student) ? "selected" : "";
            var clsName = hide + " " + selected;
            return <tr key={month + day.day} className={clsName}>
              <td>
                <Link to={"/students/" + this.state.studentId + "/" + day.day}
                      onClick={this.openMonth.bind(this, month)}
                      id={"day-"+day.day}
                      className={this.getDayClass(day)}>
                  {day.day} {this.getDayStatus(day)}
                </Link>
              </td>
            </tr>;
        }.bind(this));
    };

    getPreviousDays = () => {
        var selectedDay = this.props.params.day;
        if (!selectedDay && this.state.day) {
            //routerc.get().transitionTo('swipes', {studentId :this.state.studentId, day: this.state.day});
        }
        var groupedDays = this.state.student.days.groupBy(groupingFunc);
        var months = Object.keys(groupedDays);
        // This returns a list of lists (of lists?), which React appears to flatten.
        return months.map(function(month){
            var cls = (month===this.state.selectedMonth)
                    ? "glyphicon glyphicon-chevron-down"
                    : "glyphicon glyphicon-chevron-right";
            return [
              <tr key={month} style={{fontWeight:"bold"}}>
                <td onClick={this.toggleMonth.bind(this, month)}
                    style={{fontWeight:"bold"}}>
                  <span className={cls}
                        style={{"paddingRight":"3px"}} ></span>
                  {month}
                </td>
              </tr>,
              this.listMonth(groupedDays[month], (this.state.selectedMonth == month), month)
            ];
        }.bind(this));
    };

    toggleEdit = () => {
        if(userStore.isAdmin()) {
            this.refs.studentEditor.edit(this.state.student);
        }
    };

    getActiveDay = (student) => {
        if (this.props.params.day) {
            return this.props.params.day;
        } else if (student && student.days[0]) {
            return student.days[0].day;
        } else {
            return '';
        }
    };

    toggleHours = () => {
        this.state.student.olderdate = !!!this.state.student.olderdate;
        actionCreator.toggleHours(this.state.student._id);
    };

    showingStudentName = () => {
        return <div className="col-sm-8" id="studentName" >
              <span id="edit-name" >
                <h1 className="pull-left">{this.state.student.name}</h1>
                <span className="fa fa-pencil edit-student"></span>
              </span>

              <h2 className="badge badge-red">{(!this.studentInToday() && this.state.student.absent_today) ? 'Absent' : ''}</h2>
        </div>;
    };

    render() {
        if (this.state.student) {
            var activeDate = this.getActiveDay(this.state.student);
            var attended = (this.state.student.total_days + this.state.student.total_short).toString()
                         + " (" + this.state.student.total_short +  ")",
                requiredMinutes = this.state.student.required_minutes;
            return <div className="row">

          <StudentEditor ref="studentEditor">
          </StudentEditor>
          <SwipeHelpers ref="missingSwipeCollector">
          </SwipeHelpers>

          <div className="col-sm-1"></div>
          <div className="col-sm-10">
            <div className="panel panel-info" >
              <div className="panel-heading">
                <div className="row" onClick={this.toggleEdit}>
                  { this.showingStudentName() }
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

    }

    _onChange = () => {
        var s = studentStore.getStudent(this.state.studentId);

        var activeDay = this.getActiveDay(s);
        this.setState({
            student: s,
            selectedMonth : "2016-04",
            activeDay: activeDay
        });
    };
}

module.exports = Student;
