var React = require("react"),
  Heatmapmonth = require("./heatmapmonth.jsx");

Array.prototype.concatAll = function () {
  var results = [];
  this.forEach(function (subArray) {
    subArray.forEach(function (subArrayValue) {
      results.push(subArrayValue);
    });
  });
  return results;
};

var groupingFunc = function (data) {
  return data.day.split("-")[0] + "-" + data.day.split("-")[1] + "-" + "01";
};

module.exports = class extends React.Component {
  static displayName = "Heatmap";

  loadHeatmaps = () => {
    var groupedDays = this.props.days.groupBy(groupingFunc);
    var minutes = this.props.requiredMinutes;
    var sortedDates = Object.keys(groupedDays).sort();

    var i,
      j,
      temparray,
      chunk = 4;
    var maps = [];
    for (i = 0, j = sortedDates.length; i < j; i += chunk) {
      temparray = sortedDates.slice(i, i + chunk);
      var dates = temparray
        .map(function (d) {
          return groupedDays[d];
        })
        .concatAll();
      var m = this.makeHeatmapRow(dates, minutes, i);
      maps.push(m);
    }
    return maps;
  };

  makeHeatmapRow = (dates, minutes, key) => {
    return (
      <Heatmapmonth key={key} index={key} requiredMinutes={minutes} days={dates}></Heatmapmonth>
    );
  };

  render() {
    return <div className="row">{this.loadHeatmaps()}</div>;
  }

  componentDidMount() {
    //this.loadHeatmap();
  }

  componentDidUpdate() {
    //this.loadHeatmap();
  }
};
