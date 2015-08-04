var React = require('react'),
    Router = require('react-router'),
    Link = Router.Link,
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

        var rows = this.state.students.map(function (student) {
            var link = <Link to="students" params={{studentId: student._id}}>{student.name}</Link>;
            if (student.absent_today) {
                absentCol.push(<span className="student-listing col-sm-4">
                {link}
            </span>);
            }
            else if (!student.in_today && !student.absent_today) {
                notYetInCol.push(<span className="student-listing col-sm-4">
                {link}
            </span>);
            }
            else if (student.in_today && student.last_swipe_type === 'in') {
                inCol.push(<span className="student-listing col-sm-4">
                {link}
            </span>);
            }
            else if (student.in_today && student.last_swipe_type === 'out') {
                outCol.push(<span className="student-listing col-sm-4">
                {link}
            </span>);
            }
        });

        return <div className="row student-listing-table">
            <div className="col-sm-3"><h2>Not Coming In</h2>

                <div className="row">{absentCol}</div>
            </div>
            <div className="col-sm-3"><h2>Not Yet In</h2>

                <div className="row">{notYetInCol}</div>
            </div>
            <div className="col-sm-3"><h2>In</h2>

                <div className="row">{inCol}</div>
            </div>
            <div className="col-sm-3"><h2>Out</h2>

                <div className="row">{outCol}</div>
            </div>
        </div>;
    },
    _onChange: function () {
        this.setState({students: studentStore.getStudents()});
    }
})
;
