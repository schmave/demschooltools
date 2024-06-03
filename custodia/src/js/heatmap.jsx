const React = require("react");
const Heatmapmonth = require("./heatmapmonth.jsx");

Array.prototype.concatAll = function () {
  const results = [];
  this.forEach(function (subArray) {
    subArray.forEach(function (subArrayValue) {
      results.push(subArrayValue);
    });
  });
  return results;
};

const groupingFunc = function (data) {
  return data.day.split("-")[0] + "-" + data.day.split("-")[1] + "-" + "01";
};

module.exports = class Heatmap extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      mounted: false,
    };

    // Defer rendering the heatmaps so that the initial page load can take
    // place without waiting for them.
    setTimeout(() => {
      this.setState({ mounted: true });
    }, 0);
  }

  loadHeatmaps = () => {
    const groupedDays = this.props.days.groupBy(groupingFunc);
    const minutes = this.props.requiredMinutes;
    const sortedDates = Object.keys(groupedDays).sort();

    let i;
    let j;
    let temparray;
    const chunk = 4;
    const maps = [];
    for (i = 0, j = sortedDates.length; i < j; i += chunk) {
      temparray = sortedDates.slice(i, i + chunk);
      const dates = temparray
        .map(function (d) {
          return groupedDays[d];
        })
        .concatAll();
      const m = this.makeHeatmapRow(dates, minutes, i);
      maps.push(m);
    }
    return maps;
  };

  makeHeatmapRow = (dates, minutes, key) => {
    return <Heatmapmonth key={key} index={key} requiredMinutes={minutes} days={dates} />;
  };

  render() {
    return this.state.mounted && <div className="row">{this.loadHeatmaps()}</div>;
  }
};
