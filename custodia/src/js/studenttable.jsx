var React = require('react'),
    Router = require('react-router'),
    Link = Router.Link,
    AdminItem = require('./adminwrapper.jsx'),
    actionCreator  = require('./studentactioncreator'),
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
            <div className="col-sm-2 column"><h2>Not Coming In</h2>
                <div className="row">{absentCol}</div>
            </div>
            <div className="col-sm-3 column"><h2>Not Yet In</h2>
                <div className="row">{notYetInCol}</div>
            </div>
            <div className="col-sm-2 column"><h2>In</h2>
                <div className="row">{inCol}</div>
            </div>
            <div className="col-sm-2 column"><h2>Out</h2>

                <div className="row">{outCol}</div>
            </div>
            <AdminItem>
                <div className="col-sm-1 column">
                    <h2>Administration</h2>
                    <Link to="create">Add Student</Link>
                </div>
            </AdminItem>
        </div>;
    },
    _onChange: function () {
        this.setState({students: studentStore.getStudents()});
    }
});
