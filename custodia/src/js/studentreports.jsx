var reportStore = require('./reportstore'),
    Griddle = require('griddle-react');

// Table data as a list of array.
var rows = [];

function rowGetter(rowIndex) {
    return rows[rowIndex];
}

function loadState(){

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
    componentWillUnmount: function(){
        reportStore.removeChangeListener(this._onChange);
    },
    _onChange: function () {
        var years = reportStore.getSchoolYears();
        var currentYear = this.state.currentYear || years.current_year;
        this.setState({years: years, currentYear: currentYear, rows: reportStore.getReport(currentYear)});
    },
    yearSelected: function (event) {
        var currentYear = event.target.value;
        var report = reportStore.getReport(currentYear);
        this.setState({currentYear: currentYear, rows: report});
    },
    render: function () {
        return <div>
            <select onChange={this.yearSelected} value={this.state.currentYear}>
                {this.state.years ? this.state.years.years.map(function (year) {
                    return <option value={year}>{year === this.state.years.current_year ? year + " (Current)" : year}</option>;
                }.bind(this)) : ""}
            </select>
            <Griddle results={this.state.rows} resultsPerPage="50"
                     columns={['name', 'overrides', 'unexcused', 'excuses', 'short', 'total_hours']}
                     columnMetadata={[{displayName: 'Name', columnName: 'name'},
                     {displayName: 'Attended (Overrides)', columnName: 'overrides'},
                     {displayName: 'Excused Absence', columnName: 'excuses'},
                     {displayName: 'Short', columnName: 'short'},
                     {displayName: 'Total Hours', columnName: 'total_hours'}
                     ]}/>

        </div>;
    }
});

module.exports = exports;