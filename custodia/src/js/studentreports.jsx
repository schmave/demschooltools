var reportStore = require('./reportstore'),
    classStore = require('./classstore'),
    Modal = require('./modal.jsx'),
    DatePicker = require('react-widgets').DateTimePicker,
    actionCreator = require('./reportactioncreator'),
    React = require('react'),
    Griddle = require('griddle-react');

// Table data as a list of array.
var rows = [];
var getState = function () {
    return {rows: rows,
            classes: classStore.getClasses().classes,
            years: reportStore.getSchoolYears(),
            selectedClass: null};
};

var exports = React.createClass({
    getInitialState: function () {
        return getState();
    },
    componentDidMount: function () {
        reportStore.addChangeListener(this._onReportChange);
        classStore.addChangeListener(this._onClassChange);
    },
    componentWillUnmount: function () {
        reportStore.removeChangeListener(this._onReportChange);
        classStore.removeChangeListener(this._onClassChange);
    },
    _onClassChange : function() {
        this.refs.newSchoolYear.hide();
        var state = getState(),
            classes = state.classes || [],
            selectedClass = this.state.selectedClass,
            id = this.state.selectedClassId,
            matching = classes.filter(cls => cls._id === id),
            selectedClass = matching[0] || classes[0];
        if(classes != [] && !this.state.selectedClassId) {
            selectedClass = classes.filter(cls => cls.active == true)[0];
        }
        state.selectedClass = selectedClass || {};
        state.selectedClassId = this.state.selectedClass ? this.state.selectedClass._id : null;
        this.setState(state);
    },
    _onReportChange: function () {
        this.refs.newSchoolYear.hide();
        var state = getState(),
            years = state.years,
            yearExists = years.years.indexOf(this.state.currentYear) !== -1,
            currentYear = (yearExists && this.state.currentYear) ? this.state.currentYear : years.current_year,
            currentClassId = this.state.selectedClassId || null;

        state.currentYear = currentYear;
        var report = reportStore.getReport(currentYear, currentClassId);
        state.rows = report || [];
        state.loading = report=="loading";
        this.setState(state);
    },
    classSelected: function (event) {
        var currentClassId = event.target.value,
            report = reportStore.getReport(this.state.currentYear, currentClassId),
            rows = report || [],
            matching = this.state.classes.filter(function(cls) {return cls._id == currentClassId;}),

            selectedClass = (matching[0]) ? matching[0] : this.state.classes[0];
        this.setState({loading: report,
                       selectedClass: selectedClass,
                       selectedClassId: selectedClass._id,
                       rows: rows});
    },
    yearSelected: function (event) {
        var currentYear = event.target.value,
            report = reportStore.getReport(currentYear, this.state.selectedClassId),
            rows = report || [];
        this.setState({loading: report,
                       currentYear: currentYear,
                       rows: rows});
    },
    createPeriod: function(){
        actionCreator.createPeriod(this.refs.newPeriodStartDate.state.value, this.refs.newPeriodEndDate.state.value);
    },
    deletePeriod: function(){
        actionCreator.deletePeriod(this.state.currentYear);
    },
    render: function () {
        var grid = null;
        if(this.state.loading) {
                grid = <div>Loading</div>;
        }else {
                grid = <Griddle id="test" results={this.state.rows} resultsPerPage="200"
                        columns={['name', 'good', 'overrides', 'unexcused', 'excuses', 'short', 'total_hours']}
                        columnMetadata={[{displayName: 'Name', columnName: 'name'},
                                        {displayName: 'Attended', columnName: 'good'},
                                        {displayName: 'Gave Attendance', columnName: 'overrides'},
                                        {displayName: 'Unexcused', columnName: 'unexcused'},
                                        {displayName: 'Excused Absence', columnName: 'excuses'},
                                        {displayName: 'Short', columnName: 'short'},
                                        {displayName: 'Total Hours', columnName: 'total_hours'}
                                        ]}/>;
        }
        return <div>
            <div className="row margined">
                 <select id="class-select" className="pull-left" onChange={this.classSelected} value={this.state.selectedClassId}>
                    {this.state.classes ? this.state.classes.map(function (cls) {
                        return <option value={cls._id}>{cls.name}</option>;
                    }.bind(this)) : ""}
                </select>
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
            {grid}
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
