const React = require("react");
const dayjs = require("dayjs");
const { Link } = require("react-router-dom");

const reportStore = require("./reportstore");
const Modal = require("./modal.jsx");
const actionCreator = require("./reportactioncreator");

// Table data as a list of array.
const getState = function () {
  const now = dayjs();
  return {
    rows: [],
    filterStudents: "current",
    startDate: now.startOf("month").format("YYYY-MM-DD"),
    endDate: now.endOf("month").format("YYYY-MM-DD"),
    years: reportStore.getSchoolYears(),
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

class StudentTotalComponent extends React.Component {
  render() {
    const t = deciHours(this.props.data);
    return <span>{t}</span>;
  }
}

class StudentAttendedComponent extends React.Component {
  render() {
    const { good, short } = this.props.rowData;
    return <span>{good + short}</span>;
  }
}

class StudentLinkComponent extends React.Component {
  render() {
    // url ="#speakers/" + props.rowData._id + "/" + this.props.data;
    const sid = this.props.rowData._id;
    const name = this.props.data;
    return (
      <Link to={"/students/" + sid} id={"student-" + sid}>
        {name}
      </Link>
    );
  }
}

class StudentReports extends React.Component {
  constructor(props) {
    super(props);
    this.state = getState();
    this.newSchoolYearRef = React.createRef();
  }

  componentDidMount() {
    reportStore.addChangeListener(this.onReportChange);
    reportStore.getSchoolYears(true);
    this.fetchReport();
  }

  componentWillUnmount() {
    reportStore.removeChangeListener(this.onReportChange);
  }

  onReportChange = (x) => {
    if (this.newSchoolYearRef.current) {
      this.newSchoolYearRef.current.hide();
    }
    const state = this.state;
    const years = reportStore.getSchoolYears();
    const yearExists = years.years.indexOf(state.currentYear) !== -1;
    const currentYear = yearExists && state.currentYear ? state.currentYear : years.current_year;

    state.years = years;
    state.currentYear = currentYear;

    this.setState(state, this.fetchReport);
  };

  fetchReport = () => {
    const report = reportStore.getReport(this.state.currentYear, this.state.filterStudents);
    const rows = report != "loading" ? report : [];
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
      this.setState({ currentYear: newYearName }, this.fetchReport);
    });
  };

  openNewPeriod = () => {
    if (this.newSchoolYearRef.current) {
      this.newSchoolYearRef.current.show();
    }
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

  renderRows = () => {
    return this.state.rows.map((row) => (
      <tr key={row._id}>
        <td>
          <StudentLinkComponent data={row.name} rowData={row} />
        </td>
        <td>
          <StudentAttendedComponent rowData={row} />
        </td>
        <td>{row.overrides}</td>
        <td>{row.unexcused}</td>
        <td>{row.excuses}</td>
        <td>{row.short}</td>
        <td>
          <StudentTotalComponent data={row.total_hours} />
        </td>
      </tr>
    ));
  };

  render() {
    let grid = null;
    if (this.state.loading) {
      grid = <div>Loading</div>;
    } else {
      grid = (
        <div className="table-responsive">
          <table className="table table-striped table-condensed">
            <thead>
              <tr>
                <th>Name</th>
                <th>Attended</th>
                <th>Overrides</th>
                <th>Unexcused</th>
                <th>Excused Absence</th>
                <th>Short</th>
                <th>Total Hours</th>
              </tr>
            </thead>
            <tbody>{this.renderRows()}</tbody>
          </table>
        </div>
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
              onClick={this.openNewPeriod}
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
        <Modal ref={this.newSchoolYearRef} title="Create new period">
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

module.exports = StudentReports;
