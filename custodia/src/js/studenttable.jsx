var React = require('react'),
    Router = require('react-router'),
    Link = Router.Link,
    AdminItem = require('./adminwrapper.jsx'),
    actionCreator = require('./studentactioncreator'),
    studentStore = require('./StudentStore');

module.exports = React.createClass({
    getInitialState: function () {
        return {students: []};
    },
    componentDidMount: function () {
        studentStore.addChangeListener(this._onChange);
        this.setState({students: studentStore.getStudents()});
    },
    componentWillUnmount: function () {
        studentStore.removeChangeListener(this._onChange);
    },
    render: function () {
        var absentCol = [],
            notYetInCol = [],
            inCol = [],
            outCol = [];

        this.state.students.map(function (student) {
            var link = <Link to="student" params={{studentId: student._id}}>{student.name}</Link>;
            if (student.absent_today) {
                absentCol.push(<span className="student-listing col-sm-6">
                {link}
            </span>);
            }
            else if (!student.in_today && !student.absent_today) {
                notYetInCol.push(<span className="student-listing col-sm-6">
                {link}
            </span>);
            }
            else if (student.in_today && student.last_swipe_type === 'in') {
                inCol.push(<span className="student-listing col-sm-6">
                {link}
            </span>);
            }
            else if (student.in_today && student.last_swipe_type === 'out') {
                outCol.push(<span className="student-listing col-sm-6">
                {link}
            </span>);
            }
        });

        return <div className="row student-listing-table">
            <div className="col-sm-2 column">
                <div className="panel panel-info">
                    <div className="panel-heading"><b>Not Coming In</b></div>
                    <div className="panel-body row">{absentCol}</div>
                </div>
            </div>
            <div className="col-sm-3 column">
                <div className="panel panel-info">
                    <div className="panel-heading">Not Yet In</div>
                    <div className="panel-body row">{notYetInCol}</div>
                </div>
            </div>
            <div className="col-sm-2 column">
                <div className="panel panel-info">
                    <div className="panel-heading">In</div>
                    <div className="panel-body row">{inCol}</div>
                </div>
            </div>
            <div className="col-sm-2 column">
                <div className="panel panel-info">
                    <div className="panel-heading">Out</div>
                    <div className="panel-body row">{outCol}</div>
                </div>
            </div>
            <AdminItem>
                <div className="col-sm-1 column">
                    <div className="panel panel-info">
                        <div className="panel-heading">Administration</div>
                        <div className="panel-body row">
                            <Link to="create">Add Student</Link>
                        </div>
                    </div>
                </div>
            </AdminItem>
        </div>;
    },
    _onChange: function () {
        this.setState({students: studentStore.getStudents()});
    }
});
