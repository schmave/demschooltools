import heatmap from "cal-heatmap";
import "cal-heatmap/cal-heatmap.css";
import dayjs from "dayjs";
import $ from "jquery";
import React from "react";

class HeatmapMonth extends React.Component {
  map = null;

  formatDays = (days, requiredMinutes) => {
    const formatted = {};
    days.forEach(function (day) {
      formatted[dayjs(day.day).unix()] = day.total_mins;
      if (day.excused || day.override) {
        formatted[dayjs(day.day).unix()] = requiredMinutes;
      } else if (!day.absent && day.total_mins == 0) {
        formatted[dayjs(day.day).unix()] = 1;
      }
    });
    return formatted;
  };

  getHighlights = (days) => days.filter((day) => day.excused).map((day) => dayjs(day.day).toDate());

  loadHeatmap = () => {
    const data = this.formatDays(this.props.days, this.props.requiredMinutes);
    const highlight = this.getHighlights(this.props.days);

    const doUpdate = this.map !== null;
    if (!doUpdate) {
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
      this.map.highlight(highlight);
    } else {
      this.map.init({
        itemSelector: selector,
        onClick: function (d) {
          $(makeDateId(d))[0]?.click();
        },
        data,
        start: dayjs(this.props.days[0].day).startOf("month").toDate(),
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
    return <div id={"heatmap" + this.props.index} style={{ float: "none" }}></div>;
  }

  componentDidMount() {
    this.loadHeatmap();
  }

  componentDidUpdate() {
    this.loadHeatmap();
  }
}

export default HeatmapMonth;
