import "cal-heatmap/cal-heatmap.css";
import dayjs from "dayjs";
import { useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";

// TODO:
// Set start of week to Monday
// Make legend more understandable
// heatmap click handler doesn't seem to reload on hot reload

const HeatmapMonth = ({ days, requiredMinutes, index }) => {
  const mapRef = useRef(null);
  const navigate = useNavigate();
  const location = useLocation();

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
    const loadHeatmap = async () => {
      const data = formatDays(days, requiredMinutes);
      const highlight = getHighlights(days);

      const doUpdate = mapRef.current !== null;
      if (!doUpdate) {
        const CalHeatmap = (await import("cal-heatmap")).default;
        mapRef.current = new CalHeatmap();
      }

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

      if (doUpdate) {
        await mapRef.current.fill(data);
        await mapRef.current.paint({
          date: {
            highlight,
          },
        });
      } else {
        const Tooltip = (await import("cal-heatmap/plugins/Tooltip")).default;
        const Legend = (await import("cal-heatmap/plugins/Legend")).default;

        await mapRef.current.paint(
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
                domain: [1, 210, requiredMinutes - 45, requiredMinutes - 15].sort((a, b) => a - b),
              },
            },
          },
          [
            [
              Tooltip,
              {
                text: function (date, value, dayjsDate) {
                  return (value ? value : "0") + " minute" + (value !== 1 ? "s" : "");
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
          ],
        );
        mapRef.current.on("click", (event, timestamp, value) => {
          const d = new Date(timestamp);
          document.getElementById(makeDateId(d))?.click();
        });
      }
    };

    loadHeatmap();
  }, [days, requiredMinutes, index, navigate, location.pathname]);

  return <div id={"heatmap" + index} style={{ float: "none" }}></div>;
};

export default HeatmapMonth;
