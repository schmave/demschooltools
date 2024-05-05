const React = require("react");
const heatmap = require("cal-heatmap");
const moment = require("moment");

class HeatmapMonth extends React.Component {
  map = null;

  formatDays = (days, requiredMinutes) => {
    const formatted = {};
    days.forEach(function (day) {
      formatted[moment(day.day).unix()] = day.total_mins;
      if (day.excused || day.override) {
        formatted[moment(day.day).unix()] = requiredMinutes;
      } else if (!day.absent && day.total_mins == 0) {
        formatted[moment(day.day).unix()] = 1;
      }
    });
    return formatted;
  };

  getHighlights = (days, requiredMinutes) => {
    const hights = [];
    days.forEach(function (day) {
      if (day.excused) {
        hights.push(moment(day.day).toDate());
      }
    });
    return hights;
  };

  loadHeatmap = () => {
    const data = this.formatDays(this.props.days, this.props.requiredMinutes);
    const highlight = this.getHighlights(this.props.days, this.props.requiredMinutes);

    const doUpdate = this.map !== null;
    if (!doUpdate) {
      // eslint-disable-next-line new-cap
      this.map = new heatmap();
    }

    const selector = "#heatmap" + this.props.index;
    const padNumber = function (n) {
      return ("0" + n).slice(-2);
    };
    const makeDateId = function (d) {
      const datestring =
        d.getFullYear() + "-" + padNumber(d.getMonth() + 1) + "-" + padNumber(d.getDate());

      return "#day-" + datestring;
    };
    if (doUpdate) {
      this.map.update(data);
    } else {
      this.map.init({
        itemSelector: selector,
        onClick: function (d, nb) {
          $(makeDateId(d))[0].click();
        },
        data,
        start: moment(this.props.days[0].day).startOf("month").toDate(),
        domain: "month",
        subDomain: "x_day",
        subDomainTextFormat: "%d",
        range: 4,
        legend: [
          0,
          210,
          this.props.requiredMinutes - 45,
          this.props.requiredMinutes - 15,
          this.props.requiredMinutes - 1,
        ],
        legendVerticalPosition: "center",
        legendCellSize: 8,
        itemName: ["minute", "minutes"],
        legendOrientation: "vertical",
        highlight,
        cellSize: 15,
      });
    }
  };

  render() {
    return (
      <div id={"heatmap" + this.props.index} className="col-sm-4" style={{ float: "none" }}></div>
    );
  }

  componentDidMount() {
    this.loadHeatmap();
  }

  componentDidUpdate() {
    this.loadHeatmap();
  }
}

module.exports = HeatmapMonth;
