import "cal-heatmap/cal-heatmap.css";
import dayjs from "dayjs";
import { useEffect } from "react";

const HeatmapMonth = ({ days, requiredMinutes, index, showLegend }) => {
  const formatDays = (days, requiredMinutes) => {
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

  const getHighlights = (days) =>
    days.filter((day) => day.excused).map((day) => dayjs(day.day).toDate());

  useEffect(() => {
    let heatmap = null;

    const loadHeatmap = async () => {
      const data = formatDays(days, requiredMinutes);
      const highlight = getHighlights(days);

      const CalHeatmap = (await import("cal-heatmap")).default;
      heatmap = new CalHeatmap();
      const selector = "#heatmap" + index;
      const padNumber = function (n) {
        return ("0" + n).slice(-2);
      };
      const makeDateId = function (d) {
        const datestring =
          d.getUTCFullYear() +
          "-" +
          padNumber(d.getUTCMonth() + 1) +
          "-" +
          padNumber(d.getUTCDate());

        return "day-" + datestring;
      };

      const Tooltip = (await import("cal-heatmap/plugins/Tooltip")).default;
      const Legend = (await import("cal-heatmap/plugins/Legend")).default;

      await heatmap.paint(
        {
          itemSelector: selector,
          data: {
            source: data,
            x: "date",
            y: "value",
          },
          date: {
            start: dayjs(days[0].day).startOf("month").toDate(),
            highlight,
            locale: { weekStart: 1 },
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
              domain: [0, 210, requiredMinutes - 45, requiredMinutes - 15, requiredMinutes].sort(
                (a, b) => a - b,
              ),
            },
          },
        },
        [
          [
            Tooltip,
            {
              text: function (date, value, dayjsDate) {
                if (value === null || value === undefined) {
                  return null;
                }
                const dow = dayjsDate.format("ddd MMM D: ");
                return dow + (value ? value : "0") + " minute" + (value !== 1 ? "s" : "");
              },
            },
          ],
          showLegend && [
            Legend,
            {
              itemSelector: selector,
              label: "minutes",
              width: 200,
            },
          ],
        ].filter((x) => Boolean(x)),
      );
      heatmap.on("click", (event, timestamp, _value) => {
        const d = new Date(timestamp);
        document.getElementById(makeDateId(d))?.click();
      });
    };

    loadHeatmap();

    return () => {
      heatmap?.destroy();
    };
  }, [days, requiredMinutes, index]);

  return <div id={"heatmap" + index} style={{ float: "none" }}></div>;
};

export default HeatmapMonth;
