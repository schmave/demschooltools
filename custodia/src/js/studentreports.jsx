var reportStore = require('./reportstore'),
    FixedDataTable = require('fixed-data-table'),
    Griddle = require('griddle-react');

// Table data as a list of array.
var rows = [
    ['a1', 'b1', 'c1'],
    ['a2', 'b3', 'c2'],
    ['a3', 'b3', 'c3'],
];

function rowGetter(rowIndex) {
    return rows[rowIndex];
}

var exports = React.createClass({
    getInitialState: function(){
        return {rows: rows};
    },
    componentDidMount: function(){
        this.setState({years:reportStore.getSchoolYears()});
        reportStore.addChangeListener(this._onChange);
    },
    _onChange: function(){
        var years = reportStore.getSchoolYears();
        var currentYear = this.state.currentYear || years.currentYear;
        this.setState({years: years, currentYear: currentYear, rows: reportStore.getReport(currentYear)});
    },
    yearSelected: function(event){
        var currentYear = event.target.value;
        reportStore.getReport(currentYear);
        this.setState({currentYear: currentYear});
    },
    render: function () {
        return <div>
            <select onChange={this.yearSelected} value={this.state.currentYear}>
                {this.state.years ? this.state.years.years.map(function(year){
                    return <option>{year}</option>;
                }) : ""}
            </select>
            <Griddle results={this.state.rows}/>
        </div>;
    }
});

module.exports = exports;