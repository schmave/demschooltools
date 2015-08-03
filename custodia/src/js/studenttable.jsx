var React = require('react'),
    studentStore = require('./StudentStore');

module.exports = React.createClass({
    getInitialState: function(){
        return {students: []};
    },
    componentDidMount: function(){
        studentStore.addChangeListener(this._onChange);
        this.setState({students: studentStore.getStudents()});
    },
    render: function () {
        return <div>{this.state.students}</div>;
    },
    _onChange: function(){
        this.setState({students: studentStore.getStudents()});
    }
});
