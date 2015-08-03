var React = require('react'),
    studentStore = require('./StudentStore');

module.exports = React.createClass({
    getInitialState: function () {
        return {students: []};
    },
    componentDidMount: function () {
        studentStore.addChangeListener(this._onChange);
        this.setState({students: studentStore.getStudents()});
    },
    render: function () {
        var absentCol = [],
            notYetInCol = [],
            inCol = [],
            outCol = [];

        var rows = this.state.students.map(function (student) {
            if(student.absent_today) absentCol.push(<span className="student-listing col-sm-4">{student.name}</span>);
            if(!student.in_today && !student.absent_today) notYetInCol.push(<span className="student-listing col-sm-4">{student.name}</span>);
            if(student.in_today) inCol.push(<span className="student-listing col-sm-4">{student.name}</span>);
            if(student.absent_today) outCol.push(<span className="student-listing col-sm-4">{student.name}</span>);
        });

        return <div className="row student-listing-table">
                   <div className="col-sm-3"><h2>Not Coming In</h2><div className="row">{absentCol}</div></div>
                   <div className="col-sm-3"><h2>Not Yet In</h2><div className="row">{notYetInCol}</div></div>
                   <div className="col-sm-3"><h2>In</h2><div className="row">{inCol}</div></div>
                   <div className="col-sm-3"><h2>Out</h2><div className="row">{outCol}</div></div>
            </div>;
    },
    _onChange: function () {
        this.setState({students: studentStore.getStudents()});
    }
});
