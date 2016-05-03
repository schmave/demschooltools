var React = require('react'),
    heatmap = require('cal-heatmap'),
    moment = require('moment');

var exports = React.createClass({
    map: null,
    formatDays: function (days) {
        var formatted = {};
        days.forEach(function (day) {
            formatted[moment(day.day).unix()] = day.total_mins;
            if(day.excused) {
                formatted[moment(day.day).unix()] = 285;
            }
            if(!day.absent && day.total_mins == 0) {
                formatted[moment(day.day).unix()] = 1;
            }
        });
        return formatted;
    },
    loadHeatmap: function () {
        var data = this.formatDays(this.props.days);
        if (this.map) {
            this.map = this.map.destroy();
        }
        this.map = new heatmap();

        var selector = '#heatmap'+ this.props.index,
            padNumber = function(n) {
                return ("0" + n).slice(-2);
            },
            makeDateId = function(d) {
                var datestring = d.getFullYear() + "-" + padNumber(d.getMonth()+1) + "-" + padNumber(d.getDate());

                return "#day-"+datestring;
            };
        this.map.init({
            itemSelector: selector,
            onClick: function(d, nb) {
                $(makeDateId(d))[0].click();
            },
            data: data,
            start: moment(this.props.days[0].day).startOf('month').toDate(),
            domain: 'month',
            subDomain: 'x_day',
            subDomainTextFormat: "%d",
            range: 4,
            legend: [0, (this.props.requiredMinutes - 15), (this.props.requiredMinutes-1),500],
            legendVerticalPosition: 'center',
            legendCellSize: 8,
            itemName: ['minute', 'minutes'],
            legendOrientation: 'vertical',
            highlight: ['now'],
            cellSize: 15
        });
    },
    render: function () {
        return <div id={"heatmap" + this.props.index} className="col-sm-4"></div>;
    },
    componentDidMount: function () {
        this.loadHeatmap();
    }
});

module.exports = exports;
