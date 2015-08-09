var React = require('react'),
    studentStore = require('./StudentStore');

var swipes;

var exports = React.createClass({
    contextTypes: {
        router: React.PropTypes.func
    },
    getSwipesForDay: function () {
        var swipeRows = [];
        if(swipes) {
            swipes.map(function (swipe) {
                swipeRows.push(<tr>
                    <td>{swipe.nice_in_time}</td>
                    <td>{swipe.nice_out_time}</td>
                </tr>)
            })
        }
        return swipeRows;
    },
    render: function () {
        var student = studentStore.getStudent(this.context.router.getCurrentParams().studentId);
        swipes = student.days.find(function(day){
            return day.day === this.context.router.getCurrentParams().day;
        }.bind(this)).swipes;

        return <table className="table table-striped center">
            <thead>
            <tr>
                <th className="center">In Time</th>
                <th className="center">Out Time</th>
            </tr>
            </thead>
            <tbody>
            {this.getSwipesForDay()}
            </tbody>
        </table>;
    }
});

module.exports = exports;