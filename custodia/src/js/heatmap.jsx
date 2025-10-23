import dayjs from "dayjs";
import { useEffect, useMemo, useState } from "react";

import Heatmapmonth from "./heatmapmonth.jsx";

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

export default function Heatmap({ days, requiredMinutes }) {
  const [mounted, setMounted] = useState(false);

  // Defer rendering the heatmaps so that the initial page load can take
  // place without waiting for them.
  useEffect(() => {
    window.setTimeout(() => {
      setMounted(true);
    }, 0);
  }, []);

  const heatmaps = useMemo(() => {
    const groupedDays = days.groupBy(groupingFunc);
    const sortedDates = Object.keys(groupedDays).sort();

    let i = 0;
    const maps = [];
    while (i < sortedDates.length) {
      // Find up to four months worth of dates, which we will send
      // to Heatmapmonth in one batch.
      let j = i + 1;
      const lastDate = dayjs(sortedDates[i]).add(4, "months");
      while (j < sortedDates.length && dayjs(sortedDates[j]).isBefore(lastDate)) {
        j++;
      }

      const dates = sortedDates
        .slice(i, j)
        .map(function (d) {
          return groupedDays[d];
        })
        .concatAll();
      i = j;

      maps.push(<Heatmapmonth key={i} index={i} requiredMinutes={requiredMinutes} days={dates} />);
    }
    return maps;
  }, [days, requiredMinutes]);

  return mounted && <div className="row">{heatmaps}</div>;
}
