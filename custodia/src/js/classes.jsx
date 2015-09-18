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
        return {classes:[], students: []};
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
                       selectedClass: (classes)?classes[0]:null,
                       students: students});
    },
    classSelected: function (classval) {
        this.setState({selectedClass: classval});
    },
    createClass: function(){
        actionCreator.createClass("test class");
    },
    getStudentRowsInCurrentClass : function(){
        var rows = [];
        if(this.state.selectedClass && this.state.selectedClass.students) {
            this.state.selectedClass.students.map(function (stu, i) {
                rows.push(<tr key={stu.student_id}> <td>{stu.name}</td> </tr>);
            }.bind(this));
        }
        return rows;
    },
    classRows : function(){
        if(this.state.classes && this.state.classes.length !== 0) {
            return this.state.classes.map(function (classval, i) {
                var boundClick = this.classSelected.bind(this, classval);
                return <tr key={classval._id} onClick={boundClick} className={(classval._id === this.state.selectedClass._id)  ? "selected" : ""}>
                        <td>{classval.name}</td>
                    </tr>;
            }.bind(this));
        }
    },
    selectedStudentContains: function(stu) {
        if(this.state.selectedClass && this.state.selectedClass.students) {
            var t = this.state.selectedClass.students.some(function(istu){
                return istu.student_id === stu._id;
            });
            return !t;
        }
        return false;
    },
    getStudentRowsNotInCurrentClass : function() {
        if(this.state.students && this.state.students.length !== 0) {
            var filtered = this.state.students.filter(this.selectedStudentContains);
            return filtered.map(function (stu, i) {
                return <tr key={stu._id}> <td>{stu.name} - {stu._id}</td> </tr>;
            }.bind(this));
        }
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
                    <table className="table table-striped center">
                        <thead>
                            <tr>
                                <th className="center">Students</th>
                            </tr>
                        </thead>
            <tbody> {this.getStudentRowsInCurrentClass()} </tbody>
                    </table>
                </div>
                <div className="col-sm-2 column">
                    <table className="table table-striped center">
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
