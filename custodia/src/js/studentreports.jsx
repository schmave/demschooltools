const reportStore = require("./reportstore");
const Modal = require("./modal.jsx");
const Router = require("react-router");
const Link = Router.Link;
const actionCreator = require("./reportactioncreator");
const React = require("react");
const Griddle = require("griddle-react");

// Table data as a list of array.
const getState = function () {
  return {
    rows: [],
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
    let good = this.props.rowData.good;
    const short = this.props.rowData.short;

    good = good + short + " (" + short + ")";
    return <span>{good}</span>;
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
  state = getState();

  componentDidMount() {
    reportStore.addChangeListener(this.onReportChange);
    reportStore.getSchoolYears(true);
    this.fetchReport(this.state.currentYear);
  }

  componentWillUnmount() {
    reportStore.removeChangeListener(this.onReportChange);
  }

  onClassChange = () => {
    this.refs.newSchoolYear.hide();
    const state = this.state;
    this.setState(state);
    this.fetchReport(state.currentYear);
  };

  onReportChange = (x) => {
    this.refs.newSchoolYear.hide();
    const state = this.state;
    const years = reportStore.getSchoolYears();
    const yearExists = years.years.indexOf(state.currentYear) !== -1;
    const currentYear = yearExists && state.currentYear ? state.currentYear : years.current_year;

    state.years = years;
    state.currentYear = currentYear;

    this.setState(state);
    this.fetchReport(currentYear);
  };

  fetchReport = (year) => {
    const report = reportStore.getReport(year);
    const rows = report != "loading" ? report : [];
    this.setState({
      loading: report == null || report == "loading",
      rows,
    });
  };

  yearSelected = (event) => {
    const currentYear = event.target.value;
    this.setState({ currentYear });
    this.fetchReport(currentYear);
  };

  createPeriod = () => {
    actionCreator.createPeriod(this.state.startDate, this.state.endDate);
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

  render() {
    let grid = null;
    if (this.state.loading) {
      grid = <div>Loading</div>;
    } else {
      grid = (
        <Griddle
          id="test"
          results={this.state.rows}
          resultsPerPage="200"
          columns={["name", "good", "overrides", "unexcused", "excuses", "short", "total_hours"]}
          columnMetadata={[
            {
              displayName: "Name",
              columnName: "name",
              customComponent: StudentLinkComponent,
            },
            {
              displayName: "Attended",
              customComponent: StudentAttendedComponent,
              columnName: "good",
            },
            { displayName: "Overrides", columnName: "overrides" },
            { displayName: "Unexcused", columnName: "unexcused" },
            { displayName: "Excused Absence", columnName: "excuses" },
            { displayName: "Short", columnName: "short" },
            {
              displayName: "Total Hours",
              columnName: "total_hours",
              customComponent: StudentTotalComponent,
            },
          ]}
        />
      );
    }
    return (
      <div>
        <div className="row margined">
          <div className="pull-left">Report time period:</div>
          <select className="pull-left" onChange={this.yearSelected} value={this.state.currentYear}>
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
            className="pull-left delete-button btn btn-small btn-danger fa fa-trash-o"
            onClick={this.deletePeriod}
          ></button>
          <button
            className="pull-right btn btn-small btn-success"
            onClick={function () {
              this.refs.newSchoolYear.show();
            }.bind(this)}
          >
            New Period
          </button>
        </div>
        {grid}
        <Modal ref="newSchoolYear" title="Create new period">
          <form className="form">
            <div className="form-group">
              <div className="margined">
                <label htmlFor="startDate">Start:</label>{" "}
                <input type="date" onChange={this.onStartDateChange} />
              </div>
              <div className="margined">
                <label htmlFor="endDate">End:</label>{" "}
                <input type="date" onChange={this.onEndDateChange} />
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
