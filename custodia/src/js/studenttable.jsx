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
        var rows = this.state.students.map(function (student) {
            return <tr>
                <td>{student.absent_today ? student.name : ''}</td>
                <td>{!student.in_today && !student.absent_today ? student.name : ''}</td>
                <td>{student.in_today ? student.name : ''}</td>
                <td>{student.absent_today ? student.name : ''}</td>
            </tr>;
        });
        return <table className="table">
            <thead>
                <tr>
                    <th>Not Coming In</th>
                    <th>Not Yet In</th>
                    <th>In</th>
                    <th>Out</th>
                </tr>
            </thead>
            <tbody>
                {rows}
            </tbody>
        </table>;
    },
    _onChange: function () {
        this.setState({students: studentStore.getStudents()});
    }
});
