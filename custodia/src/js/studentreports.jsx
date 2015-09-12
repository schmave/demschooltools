var reportStore = require('./reportstore'),
    Modal = require('./modal.jsx'),
    DatePicker = require('react-widgets').DateTimePicker,
    actionCreator = require('./reportactioncreator'),
    Griddle = require('griddle-react');

// Table data as a list of array.
var rows = [];

function rowGetter(rowIndex) {
    return rows[rowIndex];
}

function loadState() {

}

var exports = React.createClass({
    getInitialState: function () {
        return {rows: rows};
    },
    componentDidMount: function () {
        var years = reportStore.getSchoolYears();
        var currentYear = this.state.currentYear || (years ? years.current_year : null);
        this.setState({years: years, currentYear: currentYear, rows: reportStore.getReport(currentYear)});
        reportStore.addChangeListener(this._onChange);
    },
    componentWillUnmount: function () {
        reportStore.removeChangeListener(this._onChange);
    },
    _onChange: function () {
        this.refs.newSchoolYear.hide();
        var years = reportStore.getSchoolYears();
        var yearExists = years.years.indexOf(this.state.currentYear) !== -1;
        var currentYear = (yearExists && this.state.currentYear) ? this.state.currentYear : years.current_year;
        this.setState({years: years, currentYear: currentYear, rows: reportStore.getReport(currentYear)});
    },
    yearSelected: function (event) {
        var currentYear = event.target.value;
        var report = reportStore.getReport(currentYear);
        this.setState({currentYear: currentYear, rows: report});
    },
    createPeriod: function(){
        actionCreator.createPeriod(this.refs.newPeriodStartDate.state.value, this.refs.newPeriodEndDate.state.value);
    },
    deletePeriod: function(){
        actionCreator.deletePeriod(this.state.currentYear);
    },
    render: function () {
        return <div>
            <div className="row margined">
                <select className="pull-left" onChange={this.yearSelected} value={this.state.currentYear}>
                    {this.state.years ? this.state.years.years.map(function (year) {
                        return <option
                            value={year}>{year === this.state.years.current_year ? year + " (Current)" : year}</option>;
                    }.bind(this)) : ""}
                </select>
                <button className="pull-left delete-button btn btn-small btn-danger fa fa-trash-o" onClick={this.deletePeriod}></button>
                <button className="pull-right btn btn-small btn-success"
                        onClick={function(){this.refs.newSchoolYear.show();}.bind(this)}>New Period
                </button>
            </div>
            <Griddle results={this.state.rows} resultsPerPage="200"
                     columns={['name', 'good', 'overrides', 'unexcused', 'excuses', 'short', 'total_hours']}
                     columnMetadata={[{displayName: 'Name', columnName: 'name'},
                                      {displayName: 'Attended', columnName: 'good'},
                                      {displayName: 'Gave Attendance', columnName: 'overrides'},
                                      {displayName: 'Unexcused', columnName: 'unexcused'},
                                      {displayName: 'Excused Absence', columnName: 'excuses'},
                                      {displayName: 'Short', columnName: 'short'},
                                      {displayName: 'Total Hours', columnName: 'total_hours'}
                     ]}/>

            <Modal ref="newSchoolYear" title="Create new period">
                <form className="form-inline">
                    <div className="form-group">
                        <label htmlFor="startDate">Start:</label> <DatePicker id="startDate" ref="newPeriodStartDate" onChange={this.newPeriodDateSelected} time={false}/>
                        <label htmlFor="endDate">End:</label> <DatePicker ref="newPeriodEndDate" id="endDate" time={false}/>
                    </div>
                    <div className="form-group" style={{marginLeft: '2em'}}>
                        <button className="btn btn-sm btn-primary" onClick={this.createPeriod}>Create Period</button>
                    </div>
                </form>
            </Modal>
        </div>;
    }
});

module.exports = exports;
