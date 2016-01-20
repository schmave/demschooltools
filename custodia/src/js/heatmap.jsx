var React = require('react'),
  heatmap = require('cal-heatmap'),
  Heatmapmonth = require('./heatmapmonth.jsx');

var groupingFunc = function (data) {
    return data.day.split('-')[0] + '-' + data.day.split('-')[1] + '-' + '01';
};

module.exports = React.createClass({
    loadHeatmaps: function () {
        var groupedDays = this.props.days.groupBy(groupingFunc);
        var minutes = this.props.requiredMinutes;
        var sortedDates = Object.keys(groupedDays).sort();

        //var maps = sortedDates.map(function(date, idx){
            return <Heatmapmonth index="1" 
                                 requiredMinutes={minutes}
                                 days={this.props.days}></Heatmapmonth>;
        //});
        //return maps;
    },
    render: function () {
        return <div className="row">{this.loadHeatmaps()}</div>;
    },
    componentDidMount: function () {
        //this.loadHeatmap();
    },
    componentDidUpdate: function () {
        //this.loadHeatmap();
    }
});
