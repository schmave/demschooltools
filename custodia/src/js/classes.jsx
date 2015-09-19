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
        return {classes:[], students: [], selectedClass: {students:[]}};
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
        var t = this.state.selectedClass.students.map(function (stu) {
            return <tr key={stu.student_id}> <td>{stu.name}</td> </tr>;
        });
        if (t.length == 0) {return undefined;}
        return <tbody> {t} </tbody>;
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
    getStudentRowsNotInCurrentClass : function() {
        var filtered = this.state.students.filter(this.selectedStudentContains);
        var t = filtered.map(function (stu) {
            return <tr key={stu._id}> <td>{stu.name} - {stu._id}</td> </tr>;
        });
        if (t.length == 0) {return undefined;}
        return <tbody> {t} </tbody>;
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
             {this.getStudentRowsInCurrentClass()}
                    </table>
                </div>
                <div className="col-sm-2 column">
                    <table className="table table-striped center">
                        <thead>
                            <tr>
                                <th className="center">Students Not In Class</th>
                            </tr>
                        </thead>
             {this.getStudentRowsNotInCurrentClass()}
                    </table>
                </div>
            </div>
        </div>;
    }
});

module.exports = exports;
