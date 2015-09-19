if (!Array.prototype.some) {
    Array.prototype.some = function(fun/*, thisArg*/) {
        'use strict';

        if (this == null) {
            throw new TypeError('Array.prototype.some called on null or undefined');
        }

        if (typeof fun !== 'function') {
            throw new TypeError();
        }

        var t = Object(this);
        var len = t.length >>> 0;

        var thisArg = arguments.length >= 2 ? arguments[1] : void 0;
        for (var i = 0; i < len; i++) {
            if (i in t && fun.call(thisArg, t[i], i, t)) {
                return true;
            }
        }

        return false;
    };
}

var classStore = require('./classstore'),
    studentStore = require('./StudentStore');

var exports = React.createClass({
    getInitialState: function () {
        return {classes:[], students: [], selectedClassId: 1};
    },
    componentDidMount: function () {
        classStore.getClasses();
        classStore.addChangeListener(this._onChange);
    },
    componentWillUnmount: function () {
        classStore.removeChangeListener(this._onChange);
    },
    _onChange: function () {
        var both = classStore.getClasses(),
            classes = both ? both.classes : [],
            students = both ? both.students : [];
        this.setState({classes: classes,
                       selectedClassId: (classes)?classes[0]._id:null,
                       students: students});
    },
    classSelected: function (classId) {
        this.setState({selectedClassId: classId});
    },
    createClass: function(){
        actionCreator.createClass("test class");
    },
    getSelectedClass : function(){
        var id = this.state.selectedClassId;
        var selectedClass = this.state.classes.filter(function(cls){
            return cls._id === id;})[0];
        return selectedClass ? selectedClass : {students:[]};
    },
    classRows : function(){
        return this.state.classes.map(function (classval, i) {
            var boundClick = this.classSelected.bind(this, classval._id);
            return <tr key={classval._id}
                       onClick={boundClick}
                       className={(classval._id === this.state.selectedClassId)  ? "selected" : ""}>
                <td>{classval.name}</td></tr>;
        }.bind(this));
    },
    selectedStudentNotContains: function(stu) {
        return this.getSelectedClass().students.some(function(istu){
            return istu.student_id === stu._id;
        });
    },
    selectedStudentContains: function(stu) {
        return !this.getSelectedClass().students.some(function(istu){
            return istu.student_id === stu._id;
        });
    },
    getStudentRowsInCurrentClass : function(){
        var filtered = this.state.students.filter(this.selectedStudentNotContains);
        return filtered.map(function (stu) {
            return <tr key={stu._id}> <td>{stu.name} - {stu._id}</td> </tr>;
        });
    },
    getStudentRowsNotInCurrentClass : function() {
        var filtered = this.state.students.filter(this.selectedStudentContains);
        return filtered.map(function (stu) {
            return <tr key={stu._id}> <td>{stu.name} - {stu._id}</td> </tr>;
        });
    },
    render: function () {
        return <div>
            <div className="row margined">
               <div className="col-sm-2 column">
                                    <table  className="table table-striped center">
                                            <thead>
                                                <tr>
                                                    <th className="center">Classes</th>
                                                </tr>
                                            </thead>
                                        <tbody> {this.classRows()} </tbody>
                                        </table>
                </div>
                <div className="col-sm-2 column">
                    <table className="test2 table table-striped center">
                        <thead>
                            <tr>
                                <th className="center">Students</th>
                            </tr>
                        </thead>
                        <tbody> {this.getStudentRowsInCurrentClass()} </tbody>
                    </table>
                </div>
            <div  className="col-sm-2 column">
                   <table  className="test table table-striped center">
                        <thead>
                            <tr>
                                <th className="center">Students Not In Class</th>
                            </tr>
                        </thead>
                        <tbody> {this.getStudentRowsNotInCurrentClass()} </tbody>
                    </table>
                </div>
            </div>
        </div>;
    }
});

module.exports = exports;
