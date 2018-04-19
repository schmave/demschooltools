var React = require('react'),
    PropTypes = require('prop-types'),
    Router = require('react-router'),
    Link = Router.Link,
    AdminItem = require('./adminwrapper.jsx'),
    actionCreator = require('./studentactioncreator'),
    myhistory = require('./myhistory.js'),
    SwipeHelpers = require('./swipeHelpers.jsx'),
    studentStore = require('./StudentStore');

module.exports = class extends React.Component {
    static displayName = "StudentTable";

    static contextTypes = {
        router: PropTypes.object.isRequired
    };

    state = {students: studentStore.getStudents(true),
            today: null};

    componentDidMount() {
        studentStore.addChangeListener(this._onChange);
    }

    componentWillUnmount() {
        studentStore.removeChangeListener(this._onChange);
    }

    signIn = (student) => {
        this.refs.missingSwipeCollector.validateSignDirection(student, 'in');
    };

    signOut = (student) => {
        this.refs.missingSwipeCollector.validateSignDirection(student, 'out');
    };

    isSigningIn = (student) => {
        return !student.last_swipe_date || student.last_swipe_type === 'out' || !student.last_swipe_date.startsWith(studentStore.getToday());
    };

    getSwipeButton = (student, way) => {
        var buttonIcon = 'fa-arrow-right';
        if (way === 'out') {
            buttonIcon = 'fa-arrow-left';
        }
        var iclassName = "fa " + buttonIcon + " sign-" + student._id;
        var is_teacher_class = student.is_teacher ? " is_teacher" : "";
        var button_class = "btn-default name-button" +
            (student.swiped_today_late ? " late" : "");
        var sign_function = this.isSigningIn(student) ? this.signIn : this.signOut;
        if (way === 'out') {
            return <button onClick={sign_function.bind(this, student)}
                           className={"btn btn-sm " + button_class + is_teacher_class}>
                <i className={iclassName}>&nbsp;</i>
                <span className="name-span">{student.name}</span>
             </button>;
        } else {
            return <button onClick={sign_function.bind(this, student)}
                           className={"btn btn-sm " + button_class + is_teacher_class}>
                <span className="name-span">{student.name}</span>
                <i className={iclassName}>&nbsp;</i>
             </button>;
        }
    };

    getStudent = (student, way) => {
        var link = <span className="glyphicon glyphicon-calendar"></span>;
        var button = this.getSwipeButton(student, way);
        var calendar_button_class = "btn btn-default calendar-button";
        var calendar_button = <div onClick={function() {
            myhistory.push('/students/' + student._id); }}
            className={calendar_button_class}>{link}</div>;

        if (way !== 'out') {
            return <div key={student._id} className="btn-group student-listing col-sm-11" role={"group"}>
                {calendar_button}
                {button}
            </div>;
        } else {
            return <div key={student._id} className="btn-group student-listing col-sm-11" role={"group"}>
                {button}
                {calendar_button}
            </div>;
        }
    };

    render() {
        var absentCol = [],
            notYetInCol = [],
            inCol = [],
            outCol = [];

        var students = this.state.students;
        students.sort((a, b) => {
            return (a['name'] > b['name'] ? 1 : -1);
        });
        students.map(function(student) {
            if (!student.in_today && student.absent_today) {
                absentCol.push(this.getStudent(student, 'absent'));
            }
            else if (!student.in_today && !student.absent_today) {
                notYetInCol.push(this.getStudent(student, 'notYetIn'));
            }
            else if (student.in_today && student.last_swipe_type === 'in') {
                inCol.push(this.getStudent(student, 'in'));
            }
            else if (student.in_today && student.last_swipe_type === 'out') {
                outCol.push(this.getStudent(student, 'out'));
            }
        }.bind(this));

        return <div className="row">
            <SwipeHelpers ref="missingSwipeCollector"></SwipeHelpers>
            <div className="row student-listing-table">
                <div className="col-md-3 column">
                    <div className="panel panel-info absent">
                        <div className="panel-heading absent"><b>Not Coming In ({absentCol.length})</b></div>
                        <div className="panel-body row">{absentCol}</div>
                    </div>
                </div>
                <div className="col-md-3 column not-in">
                    <div className="panel panel-info">
                        <div className="panel-heading"><b>Not Yet In ({notYetInCol.length})</b></div>
                        <div className="panel-body row">{notYetInCol}</div>
                    </div>
                </div>
                <div className="col-md-3 column in">
                    <div className="panel panel-info">
                        <div className="panel-heading"><b>In ({inCol.length})</b></div>
                        <div className="panel-body row">{inCol}</div>
                    </div>
                </div>
                <div className="col-md-3 column out">
                    <div className="panel panel-info">
                        <div className="panel-heading"><b>Out ({outCol.length})</b></div>
                        <div className="panel-body row">{outCol}</div>
                    </div>
                </div>
            </div>
        </div>;
    }

    _onChange = () => {
        this.setState({students: studentStore.getStudents(),
                       today: studentStore.getToday()});
    };
};
