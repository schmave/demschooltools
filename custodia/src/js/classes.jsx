var classStore = require('./classstore');
    
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
    createClass: function(){
        actionCreator.createClass("test class");
    },
    render: function () {
        return <div>
            <div className="row margined">
            TEST
            </div>
        </div>;
    }
});

module.exports = exports;
