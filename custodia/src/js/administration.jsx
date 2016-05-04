var React = require('react'),
    Modal = require('./modal.jsx'),
    actionCreator = require('./studentactioncreator'),
    studentStore = require('./StudentStore'),
    Router = require('react-router'),
    Link = Router.Link,
    SuperItem = require('./superwrapper.jsx');

var exports = React.createClass({
    contextTypes: {
        router: React.PropTypes.func
    },
    getInitialState: function () {
        return {};
    },
    componentDidMount: function () {
        //studentStore.addChangeListener(this._onChange);
        //this.setState({student: studentStore.getStudent(this.state.studentId)});
    },
    componentWillUnmount: function () {
        //studentStore.removeChangeListener(this._onChange);
    },
    render: function () {
        return <SuperItem>
            <h1>Administration</h1>
            </SuperItem>
    },
    _onChange: function () {
        //var s = studentStore.getStudent(this.state.studentId);

        //this.setState({
        //    student: s,
        //    selectedMonth : "2016-04",
        //    activeDay: activeDay
        //});
    }
});

module.exports = exports;
