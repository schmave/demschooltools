var reportStore = require('./reportstore'),
    classStore = require('./classstore'),
    Modal = require('./modal.jsx'),
    DatePicker = require('react-widgets').DateTimePicker,
    actionCreator = require('./reportactioncreator'),
    React = require('react'),
    Griddle = require('griddle-react');

// Table data as a list of array.
var getState = function () {
    return {rows: [],
            classesAndThings: classStore.getClasses(),
            classes: [],
            years: reportStore.getSchoolYears()};
};

var exports = React.createClass({
    getInitialState: function () {
        return getState();
    },
    componentDidMount: function () {
        reportStore.addChangeListener(this.onReportChange);
        classStore.addChangeListener(this.onClassChange);
        classStore.getClasses(true);
        reportStore.getSchoolYears(true);
        this.fetchReport(this.state.selectedClassId, this.state.currentYear);
    },
    componentWillUnmount: function () {
        reportStore.removeChangeListener(this.onReportChange);
        classStore.removeChangeListener(this.onClassChange);
    },
    onClassChange : function() {
        this.refs.newSchoolYear.hide();
        var state = getState(),
            classes = (state.classesAndThings ==[]) ? [] : state.classesAndThings.classes,
            currentSelectedClassId = this.state.selectedClassId,
            matching = classes.filter(cls => cls._id === currentSelectedClassId),
            selectedClass = matching[0] || classes[0];
        if(classes != [] && !currentSelectedClassId) {
            selectedClass = classes.filter(cls => cls.active == true)[0];
        }
        state.selectedClass = selectedClass || {};
        state.selectedClassId = state.selectedClass ? state.selectedClass._id : null;
        state.classes = classes;
        this.setState(state);
        this.fetchReport(state.selectedClassId, state.currentYear);
    },
    onReportChange: function () {
        this.refs.newSchoolYear.hide();
        var state = getState(),
            years = state.years,
            yearExists = years.years.indexOf(this.state.currentYear) !== -1,
            currentYear = (yearExists && this.state.currentYear) ? this.state.currentYear : years.current_year;

        this.setState({currentYear:currentYear});
        this.fetchReport(this.state.selectedClassId, currentYear);
    },
    fetchReport: function(currentClassId, year) {
        var report = reportStore.getReport(year, currentClassId),
            rows = (report != "loading") ? report : [];
        this.setState({loading: (report==null||report=="loading"), rows: rows});
    },
    classSelected: function (event) {
        var currentClassId = event.target.value,
            matching = this.state.classes.filter(cls => cls._id == currentClassId),
            selectedClass = (matching[0]) ? matching[0] : this.state.classes[0];
        this.setState({selectedClass: selectedClass,
                       selectedClassId: selectedClass._id});
        this.fetchReport(selectedClass._id, this.state.currentYear);
    },
    yearSelected: function (event) {
        var currentYear = event.target.value;
        this.setState({currentYear: currentYear});
        this.fetchReport(this.state.selectedClassId, currentYear);
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
