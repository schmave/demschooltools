import "cal-heatmap/cal-heatmap.css";
import dayjs from "dayjs";
import $ from "jquery";
import React from "react";

class HeatmapMonth extends React.Component {
  map = null;

  formatDays = (days, requiredMinutes) => {
    const formatted = [];
    days.forEach(function (day) {
      const date = dayjs(day.day).toDate();
      let value = day.total_mins;

      if (day.excused || day.override) {
        value = requiredMinutes;
      } else if (!day.absent && day.total_mins == 0) {
        value = 1;
      }

      formatted.push({ date, value });
    });
    return formatted;
  };

  getHighlights = (days) => days.filter((day) => day.excused).map((day) => dayjs(day.day).toDate());

  loadHeatmap = async () => {
    const data = this.formatDays(this.props.days, this.props.requiredMinutes);
    const highlight = this.getHighlights(this.props.days);

    const doUpdate = this.map !== null;
    if (!doUpdate) {
      const CalHeatmap = (await import("cal-heatmap")).default;
      this.map = new CalHeatmap();
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
      await this.map.fill(data);
      await this.map.paint({
        date: {
          highlight,
        },
      });
    } else {
      const Tooltip = (await import("cal-heatmap/plugins/Tooltip")).default;
      const Legend = (await import("cal-heatmap/plugins/Legend")).default;

      await this.map.paint(
        {
          itemSelector: selector,
          data: {
            source: data,
            x: "date",
            y: "value",
          },
          date: {
            start: dayjs(this.props.days[0].day).startOf("month").toDate(),
            highlight,
          },
          domain: {
            type: "month",
          },
          subDomain: {
            type: "xDay",
            label: "D",
            width: 15,
            height: 15,
          },
          range: 4,
          scale: {
            color: {
              type: "threshold",
              range: ["#c62828", "#ff6659", "#EEE8AA", "#80e27e", "#4caf50", "#087f23"],
              domain: [
                1,
                210,
                this.props.requiredMinutes - 45,
                this.props.requiredMinutes - 15,
              ].sort((a, b) => a - b),
            },
          },
          onClick: function (event, timestamp, value) {
            const d = new Date(timestamp);
            $(makeDateId(d))[0]?.click();
          },
        },
        [
          [
            Tooltip,
            {
              text: function (date, value, dayjsDate) {
                return (
                  (value ? value : "0") + " minute" + (value !== 1 ? "s" : "")
                );
              },
            },
          ],
          [
            Legend,
            {
              itemSelector: selector,
              label: "minutes",
              width: 200,
            },
          ],
        ]
      );
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
