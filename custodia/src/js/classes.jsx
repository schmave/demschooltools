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
    actionCreator = require('./classactioncreator')
    studentStore = require('./StudentStore');

var exports = React.createClass({
    getInitialState: function () {
        return {classes:[], students: [], selectedClass: {students:[]}};
    },
    setupState: function (state) {
        var both = state,
            classes = both.classes ? both.classes : [],
            students = both.students ? both.students : [];
        var selectedClass = this.state.selectedClass;
        if(selectedClass) {
            var id = selectedClass._id,
                matching = classes.filter(function(cls) {return cls._id === id;});
            selectedClass = (matching[0]) ? matching[0] : classes[0];
        }
        this.setState({classes: classes,
                       selectedClass: (selectedClass)?selectedClass:{students:[]},
                       students: students});
    },
    componentDidMount: function () {
        this.setupState(classStore.getClasses());
        classStore.addChangeListener(this._onChange);
    },
    componentWillUnmount: function () {
        classStore.removeChangeListener(this._onChange);
    },
    _onChange: function () {
        this.setupState(classStore.getClasses());
    },
    classSelected: function (classval) {
        this.setState({selectedClass: classval});
    },
    createClass: function(){
        actionCreator.createClass("test class");
    },
    classRows : function(){
        return this.state.classes.map(function (classval, i) {
            var boundClick = this.classSelected.bind(this, classval);
            return <tr key={classval._id}
                       onClick={boundClick}
                       className={(classval._id === this.state.selectedClass._id)  ? "selected" : ""}>
                <td>{classval.name}</td></tr>;
        }.bind(this));
    },
    selectedStudentContains: function(stu) {
        return !this.state.selectedClass.students.some(function(istu){
            return istu.student_id === stu._id;
        });
    },
    deleteFromClass:function(student) {
        actionCreator.deleteStudentFromClass(student.student_id, this.state.selectedClass._id);
    },
    addToClass:function(student) {
        actionCreator.addStudentToClass(student._id, this.state.selectedClass._id);
    },
    getStudentRowsInCurrentClass : function(){
        var t = this.state.selectedClass.students.map(function (stu) {
            return <div key={"t" + this.state.selectedClass._id + "-" + stu.student_id}  className="panel panel-info student-listing col-sm-11">
                <div>
                <span className="attendance-button">
                {stu.name}
            </span>
                <span className="attendance-button">
                <button onClick={this.deleteFromClass.bind(this, stu)} className="btn btn-sm btn-primary"><i className={"fa fa fa-arrow-right"}>&nbsp;</i></button>
            </span>
            </div>
                </div>;
        }.bind(this));
        return t;
    },
    getStudentRowsNotInCurrentClass : function() {
        var filtered = this.state.students.filter(this.selectedStudentContains);
        var t = filtered.map(function (stu) {
            return <div key={"NOTCLASS-" + stu._id} className="panel panel-info student-listing col-sm-11">
                <div>
                <span className="attendance-button">
                <button onClick={this.addToClass.bind(this, stu)} className="btn btn-sm btn-primary"><i className={"fa fa-arrow-left"}>&nbsp;</i></button>
                </span>
                <span>
            {stu.name}
            </span>
            </div>
                </div>;
        }.bind(this));
        return t;
    },
    render: function () {
        return <div>
            <div className="row margined">
                <div className="col-sm-2 column">
                    <table className="table table-striped center">
                        <thead>
                            <tr>
                                <th className="center">Classes</th>
                            </tr>
                        </thead>
                       <tbody> {this.classRows()} </tbody>
                    </table>
                </div>
                <div className="col-sm-2 column">
                   <div className="panel-body row">
                        <div className="panel-heading absent"><b>In Class</b></div>
                        {this.getStudentRowsInCurrentClass()}
                    </div>
                </div>
                <div className="col-sm-2 column">
                    <div className="panel-body row">
                       <div className="panel-heading absent"><b>Not In Class</b></div>
                        {this.getStudentRowsNotInCurrentClass()}
                    </div>
                </div>
            </div>
        </div>;
    }
});

module.exports = exports;
