const React = require("react");
const Heatmapmonth = require("./heatmapmonth.jsx");
const dayjs = require("dayjs");

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

    let i = 0;
    let temparray;
    const maps = [];
    while (i < sortedDates.length) {
      // Find up to four months worth of dates, which we will send
      // to Heatmapmonth in one batch.
      let j = i + 1;
      const lastDate = dayjs(sortedDates[i]).add(4, "months");
      while (j < sortedDates.length && dayjs(sortedDates[j]).isBefore(lastDate)) {
        j++;
      }

      temparray = sortedDates.slice(i, j);
      i = j;
      const dates = temparray
        .map(function (d) {
          return groupedDays[d];
        })
        .concatAll();

      maps.push(<Heatmapmonth key={i} index={i} requiredMinutes={minutes} days={dates} />);
    }
    return maps;
  };

  render() {
    return this.state.mounted && <div className="row">{this.loadHeatmaps()}</div>;
  }
};
