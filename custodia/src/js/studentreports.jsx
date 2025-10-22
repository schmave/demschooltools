import {
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
} from "@mui/material";
import dayjs from "dayjs";
import React from "react";
import { Link } from "react-router";

import Modal from "./modal.jsx";
import actionCreator from "./reportactioncreator.js";
import reportStore from "./reportstore";

// Table data as a list of array.
const getState = function () {
  const now = dayjs();
  return {
    rows: [],
    filterStudents: "current",
    startDate: now.startOf("month").format("YYYY-MM-DD"),
    endDate: now.endOf("month").format("YYYY-MM-DD"),
    years: reportStore.getSchoolYears(),
    showNewSchoolYearModal: false,
    orderBy: "name",
    order: "asc",
  };
};
function pad(num, size) {
  let s = num + "";
  while (s.length < size) s = "0" + s;
  return s;
}

function deciHours(time) {
  if (!time) {
    return "0:00";
  }
  const i = parseInt(time, 10);
  return i.toString() + ":" + pad(Math.round((time - i) * 60, 10), 2);
}

// Column definitions
const columns = [
  { id: "name", label: "Name", sortable: true, width: "300px" },
  { id: "attended", label: "Attended", sortable: true, width: "50px" },
  { id: "overrides", label: "Overrides", sortable: true, width: "50px" },
  { id: "unexcused", label: "Unexcused", sortable: true, width: "50px" },
  { id: "excuses", label: "Excused Absence", sortable: true, width: "50px" },
  { id: "short", label: "Short", sortable: true, width: "50px" },
  { id: "total_hours", label: "Total Hours", sortable: true, width: "50px" },
];

export default class StudentReports extends React.Component {
  state = getState();

  componentDidMount() {
    reportStore.addChangeListener(this.onReportChange);
    reportStore.getSchoolYears(true);
    this.fetchReport();
  }

  componentWillUnmount() {
    reportStore.removeChangeListener(this.onReportChange);
  }

  onReportChange = () => {
    const state = this.state;
    const years = reportStore.getSchoolYears();
    const yearExists = years.years.indexOf(state.currentYear) !== -1;
    const currentYear = yearExists && state.currentYear ? state.currentYear : years.current_year;

    state.years = years;
    state.currentYear = currentYear;
    state.showNewSchoolYearModal = false;

    this.setState(state, this.fetchReport);
  };

  fetchReport = () => {
    const report = reportStore.getReport(this.state.currentYear, this.state.filterStudents);
    let rows = report != "loading" ? report : [];

    // Add unique id field and calculated attended value
    if (Array.isArray(rows)) {
      rows = rows.map((row, index) => ({
        ...row,
        id: row._id || index,
        attended: row.good + row.short,
      }));
    }

    this.setState({
      loading: report == null || report == "loading",
      rows,
    });
  };

  yearSelected = (event) => {
    const currentYear = event.target.value;
    this.setState({ currentYear }, this.fetchReport);
  };

  createPeriod = () => {
    actionCreator.createPeriod(this.state.startDate, this.state.endDate).then((newYearName) => {
      this.setState(
        {
          currentYear: newYearName,
          showNewSchoolYearModal: false,
        },
        this.fetchReport,
      );
    });
  };

  deletePeriod = () => {
    actionCreator.deletePeriod(this.state.currentYear);
  };

  onStartDateChange = (e) => {
    this.setState({ startDate: e.target.value });
  };

  onEndDateChange = (e) => {
    this.setState({ endDate: e.target.value });
  };

  onFilterStudentsChange = (e) => {
    if (e.target.checked) {
      this.setState({ filterStudents: e.target.value }, this.fetchReport);
    }
  };

  handleRequestSort = (property) => {
    let sortAsc = this.state.orderBy === property && this.state.order === "desc";
    if (property === "name" && this.state.orderBy !== property) {
      sortAsc = true;
    }
    this.setState({
      order: sortAsc ? "asc" : "desc",
      orderBy: property,
    });
  };

  getSortedRows = () => {
    const { rows, order, orderBy } = this.state;

    return [...rows].sort((a, b) => {
      let aValue = a[orderBy];
      let bValue = b[orderBy];

      // Handle special cases
      if (orderBy === "name") {
        aValue = (aValue || "").toLowerCase();
        bValue = (bValue || "").toLowerCase();
      } else if (orderBy === "total_hours") {
        aValue = parseFloat(aValue) || 0;
        bValue = parseFloat(bValue) || 0;
      } else {
        aValue = aValue || 0;
        bValue = bValue || 0;
      }

      if (bValue < aValue) {
        return order === "asc" ? 1 : -1;
      }
      if (bValue > aValue) {
        return order === "asc" ? -1 : 1;
      }
      return 0;
    });
  };

  render() {
    let grid = null;
    if (this.state.loading) {
      grid = <div>Loading</div>;
    } else {
      const sortedRows = this.getSortedRows();

      grid = (
        <TableContainer component={Paper}>
          <Table sx={{ "& .MuiTableCell-root": { fontSize: "1rem" } }}>
            <TableHead>
              <TableRow>
                {columns.map((column) => (
                  <TableCell key={column.id} style={{ width: column.width }}>
                    {column.sortable ? (
                      <TableSortLabel
                        active={this.state.orderBy === column.id}
                        direction={this.state.orderBy === column.id ? this.state.order : "asc"}
                        onClick={() => this.handleRequestSort(column.id)}
                      >
                        {column.label}
                      </TableSortLabel>
                    ) : (
                      column.label
                    )}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {sortedRows.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>
                    <Link to={`/students/${row._id}`} id={`student-${row._id}`}>
                      {row.name}
                    </Link>
                  </TableCell>
                  <TableCell>{row.attended}</TableCell>
                  <TableCell>{row.overrides}</TableCell>
                  <TableCell>{row.unexcused}</TableCell>
                  <TableCell>{row.excuses}</TableCell>
                  <TableCell>{row.short}</TableCell>
                  <TableCell>{deciHours(row.total_hours)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      );
    }
    return (
      <div>
        <div className="row margined">
          <div className="col-sm-7 col-md-4">
            Report time period:
            <br />
            <select onChange={this.yearSelected} value={this.state.currentYear}>
              {this.state.years
                ? this.state.years.years.map(
                    function (year) {
                      return (
                        <option key={year} value={year}>
                          {year === this.state.years.current_year ? year + " (Current)" : year}
                        </option>
                      );
                    }.bind(this),
                  )
                : ""}
            </select>
            <button
              className="delete-button btn btn-small btn-danger fa fa-trash-o"
              onClick={this.deletePeriod}
            ></button>
          </div>
          <div className="col-md-2">
            <button
              className="btn btn-small btn-success"
              onClick={() => this.setState({ showNewSchoolYearModal: true })}
            >
              New Period
            </button>
          </div>
          <div className="col-sm-12 col-md-5">
            <label style={{ fontWeight: "normal" }}>
              <input
                type="radio"
                name="filterStudents"
                value="current"
                checked={this.state.filterStudents === "current"}
                onChange={this.onFilterStudentsChange}
              />{" "}
              Show only current students and staff
            </label>
            <br />
            <label style={{ fontWeight: "normal" }}>
              <input
                type="radio"
                name="filterStudents"
                value="all"
                checked={this.state.filterStudents === "all"}
                onChange={this.onFilterStudentsChange}
              />{" "}
              Show all people who have attended during this period
            </label>
          </div>
        </div>
        {grid}
        <Modal
          open={this.state.showNewSchoolYearModal}
          onClose={() => this.setState({ showNewSchoolYearModal: false })}
          title="Create new period"
        >
          <p>
            The start date, end date, and all dates in between the two will be included in the
            report.
          </p>
          <form className="form">
            <div className="form-group" style={{ display: "flex" }}>
              <div className="margined">
                <label htmlFor="startDate">Start:</label>{" "}
                <input type="date" value={this.state.startDate} onChange={this.onStartDateChange} />
              </div>
              <div className="margined">
                <label htmlFor="endDate">End:</label>{" "}
                <input type="date" value={this.state.endDate} onChange={this.onEndDateChange} />
              </div>
            </div>
            <div className="form-group" style={{ marginLeft: "2em" }}>
              <button className="btn btn-sm btn-primary" onClick={this.createPeriod}>
                Create Period
              </button>
            </div>
          </form>
        </Modal>
      </div>
    );
  }
}
