var React = require('react'),
    studentStore = require('./StudentStore');

var exports = React.createClass({
    contextTypes: {
        router: React.PropTypes.func
    },
    getInitialState: function () {
        return {studentId: this.context.router.getCurrentParams().studentId};
    },
    componentDidMount: function () {
        studentStore.addChangeListener(this._onChange)
        studentStore.getStudent(this.state.studentId);
    },
    render: function () {
        return <div>a student lives here: {this.state.student}</div>;
    },
    _onChange: function () {
        this.setState({
            student: studentStore.getStudent(this.state.studentId),
            studentId: this.state.studentId
        })
    }
});

module.exports = exports;