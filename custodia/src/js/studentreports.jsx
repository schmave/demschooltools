var reportStore = require('./reportstore'),
    classStore = require('./classstore'),
    Modal = require('./modal.jsx'),
    DatePicker = require('react-widgets').DateTimePicker,
    actionCreator = require('./reportactioncreator'),
    React = require('react'),
    Griddle = require('griddle-react');

// Table data as a list of array.
var rows = [];

var exports = React.createClass({
    getInitialState: function () {
        return {rows: rows, classes: [],selectedClass: {students:[]}};
    },
    setupClassState: function (state) {
        var both = state,
            classes = both.classes ? both.classes : [];
        var selectedClass = this.state.selectedClass;
        if(selectedClass) {
            var id = selectedClass._id,
                matching = classes.filter(function(cls) {return cls._id === id;});
            selectedClass = (matching[0]) ? matching[0] : classes[0];
        }
        this.setState({classes: classes,
                       selectedClass: (selectedClass)?selectedClass:{students:[]},
                       });
    },
    componentDidMount: function () {
        reportStore.addChangeListener(this._onReportChange);
        classStore.addChangeListener(this._onClassChange);
        this.setupClassState(classStore.getClasses());
        reportStore.getSchoolYears(true);
    },
    componentWillUnmount: function () {
        reportStore.removeChangeListener(this._onReportChange);
        classStore.removeChangeListener(this._onClassChange);
    },
    _onClassChange : function() {
        this.setupClassState(classStore.getClasses());
    },
    _onReportChange: function () {
        this.refs.newSchoolYear.hide();
        var years = reportStore.getSchoolYears();
        var yearExists = years.years.indexOf(this.state.currentYear) !== -1;
        var currentYear = (yearExists && this.state.currentYear) ? this.state.currentYear : years.current_year;
        var currentClassId = this.state.selectedClass ? this.state.selectedClass._id : null;
        this.setState({years: years, currentYear: currentYear, rows: reportStore.getReport(currentYear, currentClassId)});
    },
    classSelected: function (event) {
        var currentClassId = event.target.value;
        var report = reportStore.getReport(this.state.currentYear, currentClassId);
        var matching = this.state.classes.filter(function(cls) {return cls._id == currentClassId;});
        var selectedClass = (matching[0]) ? matching[0] : this.state.classes[0];
        this.setState({selectedClass: selectedClass, rows: report});
    },
    yearSelected: function (event) {
        var currentYear = event.target.value;
        var currentClassId = this.state.selectedClass ? this.state.selectedClass._id : null;
        var report = reportStore.getReport(currentYear, currentClassId);
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
                 <select className="pull-left" onChange={this.classSelected} value={this.state.selectedClass._id}>
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
