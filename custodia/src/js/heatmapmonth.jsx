var React = require('react'),
    heatmap = require('cal-heatmap'),
    moment = require('moment');

var exports = React.createClass({
    map: null,
    formatDays: function (days) {
        var formatted = {};
        days.forEach(function (day) {
            formatted[moment(day.day).unix()] = day.total_mins;
        });
        return formatted;
    },
    loadHeatmap: function () {
        var data = this.formatDays(this.props.days);
        if (this.map) {
            this.map = this.map.destroy();
        }
        this.map = new heatmap();

        var selector = '#heatmap'+ this.props.index;
        this.map.init({
            itemSelector: selector,
            data: data,
            start: moment(this.props.days[0].day).startOf('month').toDate(),
            domain: 'month',
            legendVerticalPosition: "center",
            legendOrientation: "vertical",
            legendMargin: [0, 10, 0, 0],
            subDomain: 'x_day',
            subDomainTextFormat: "%d",
            range: 1,
            legend: [2, 80, 160, 300],
            highlight: ['now'],
            cellSize: 15
        });
    },
    render: function () {
        return <div id={"heatmap" + this.props.index} className="col-sm-4"></div>;
    },
    componentDidMount: function () {
        this.loadHeatmap();
    },
    componentDidUpdate: function () {
        this.loadHeatmap();
    }
});

module.exports = exports;