var React = require('react'),
    studentStore = require('./StudentStore');

var exports = React.createClass({
    contextTypes: {
        router: React.PropTypes.func
    },
    getInitialState: function () {
        return {studentId: this.context.router.getCurrentParams().studentId};
    },
    render: function () {
        return <div>a student lives here: {this.state.studentId}</div>;
    }
});

module.exports = exports;