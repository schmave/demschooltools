var React = require('react'),
    heatmap = require('cal-heatmap'),
    moment = require('moment');

var map;
module.exports = React.createClass({
    formatDays: function (days) {
        var formatted = {};
        days.forEach(function (day) {
            formatted[moment(day.day).unix()] = day.total_mins;
        });
        return formatted;
    },
    loadHeatmap: function () {
        $('#heatmap').empty();
        var data = this.formatDays(this.props.days);
        if(map){
            map = map.destroy();
        }
        map = new heatmap();
        map.init({
            itemSelector: '#heatmap',
            data: data, domain: 'month',
            legendVerticalPosition: "center",
            legendOrientation: "vertical",
            legendMargin: [0, 10, 0, 0],
            subDomain: 'x_day',
            subDomainTextFormat: "%d",
            considerMissingDataAsZero: true,
            range: 6,
            cellSize: 15
        });
    },
    render: function () {
        return <div id="heatmap" className="col-sm-12">asfd</div>;
    },
    componentDidMount: function () {
        this.loadHeatmap();
    },
    componentDidUpdate: function () {
        this.loadHeatmap();
    }
});