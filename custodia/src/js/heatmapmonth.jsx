var React = require('react'),
    heatmap = require('cal-heatmap'),
    moment = require('moment');

var exports = React.createClass({
    displayName: 'HeatmapMonth',
    map: null,
    formatDays: function (days, requiredMinutes) {
        var formatted = {};
        days.forEach(function (day) {
            formatted[moment(day.day).unix()] = day.total_mins;
            if(day.excused || day.override) {
                formatted[moment(day.day).unix()] = requiredMinutes;
            } else if(!day.absent && day.total_mins == 0) {
                formatted[moment(day.day).unix()] = 1;
            }
        });
        return formatted;
    },
    getHighlights: function (days, requiredMinutes) {
        //var hights = ['now'];
        var hights = [];
        days.forEach(function (day) {
            if(day.excused) {
                hights.push(moment(day.day).toDate());
            }
        });
        return hights;
    },
    loadHeatmap: function () {
        var data = this.formatDays(this.props.days, this.props.requiredMinutes);
        var highlight = this.getHighlights(this.props.days, this.props.requiredMinutes);
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
            legend: [0, 210, this.props.requiredMinutes-45, this.props.requiredMinutes-15, this.props.requiredMinutes-1],
            legendVerticalPosition: 'center',
            legendCellSize: 8,
            itemName: ['minute', 'minutes'],
            legendOrientation: 'vertical',
            highlight: highlight,
            cellSize: 15
        });
    },
    render: function () {
        return <div id={"heatmap" + this.props.index} className="col-sm-4" style={{float: "none"}}></div>;
    },
    componentDidMount: function () {
        this.loadHeatmap();
    }
});

module.exports = exports;
