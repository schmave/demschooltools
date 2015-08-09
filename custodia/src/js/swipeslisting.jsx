var React = require('react');

var exports = React.createClass({
    getSwipesForDay: function () {
        var swipes = [];
        if(this.props.swipes) {
            this.props.swipes.map(function (swipe) {
                swipes.push(<tr>
                    <td>{swipe.nice_in_time}</td>
                    <td>{swipe.nice_out_time}</td>
                </tr>)
            })
        }
        return swipes;
    },
    render: function () {
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