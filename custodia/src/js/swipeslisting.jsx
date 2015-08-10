var React = require('react'),
    store = require('./StudentStore'),
    actionCtreator = require('./studentactioncreator'),
    studentStore = require('./StudentStore');

var exports = React.createClass({
    contextTypes: {
        router: React.PropTypes.func
    },
    componentWillUnmount: function () {
        studentStore.removeChangeListener(this._onChange);
    },
    loadDataFromUrl: function(){
        var student = studentStore.getStudent(this.context.router.getCurrentParams().studentId);

        var student = studentStore.getStudent(this.context.router.getCurrentParams().studentId);
        var day = student.days.find(function (day) {
            return day.day === this.context.router.getCurrentParams().day;
        }.bind(this));
        var date = this.context.router.getCurrentParams().day;
        return {student: student, day: day, date: date};
    },
    getInitialState: function () {
        store.addChangeListener(this._onChange);
        return this.loadDataFromUrl();
    },
    _onChange: function(){
        this.setState(this.loadDataFromUrl());
    },
    componentWillReceiveProps: function(){
        this.setState(this.loadDataFromUrl());
    },
    deleteSwipe: function (swipe) {
        console.log(swipe);
    },
    getSwipesForDay: function () {
        var swipeRows = [];
        if (this.state.day && this.state.day.swipes) {
            this.state.day.swipes.map(function (swipe) {
                swipeRows.push(<tr>
                    <td>{swipe.nice_in_time}</td>
                    <td>{swipe.nice_out_time}</td>
                    <td onClick={this.deleteSwipe.bind(this, swipe)}><a>Delete</a></td>
                </tr>)
            }.bind(this))
        }
        return swipeRows;
    },
    excuse: function (swipe) {
        actionCtreator.excuse(this.state.student._id, this.state.date);
    },
    override: function () {
        actionCtreator.override(this.state.student._id, this.state.date);
    },
    render: function () {

        return <span>
            <table className="table table-striped center">
                <thead>
                <tr>
                    <th className="center">In Time</th>
                    <th className="center">Out Time</th>
                </tr>
                </thead>
                <tbody>
                {this.getSwipesForDay()}
                </tbody>
            </table>
            {!this.state.day.override && !this.state.day.excused ? <div>
                <div className="pull-left">
                    <button type="button" onClick={this.override} className="btn btn-sm btn-info">
                        Override
                    </button>
                </div>
                <div className="pull-right">
                    <button type="button" onClick={this.excuse} className="btn btn-sm btn-info">Excuse</button>
                </div>
            </div> : ''}
        </span>;
    },


});

module.exports = exports;