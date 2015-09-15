var classStore = require('./classstore');
    
var exports = React.createClass({
    getInitialState: function () {
        return {classes:[]};
    },
    componentDidMount: function () {
        var classes = classStore.getClasses();
        this.setState({classes: classes, selectedClass: (classes)?classes[0]:null});
        classStore.addChangeListener(this._onChange);
    },
    componentWillUnmount: function () {
        classStore.removeChangeListener(this._onChange);
    },
    _onChange: function () {
        var classes = classStore.getClasses()
        this.setState({classes: classes, selectedClass: (classes)?classes[0]:null});
    },
    classSelected: function (classval) {
        this.setState({selectedClass: classval});
    },
    createClass: function(){
        actionCreator.createClass("test class");
    },
    studentRows : function(){
        var rows = [];
        if(this.state.selectedClass && this.state.selectedClass.students) {
            this.state.selectedClass.students.map(function (stu, i) {
                rows.push(<tr> <td>{stu.name}</td> </tr>);
            }.bind(this));
        }
        return rows;
    },

    classRows : function(){
        if(this.state.classes && this.state.classes.length !== 0) {
            return this.state.classes.map(function (classval, i) {
                var boundClick = this.classSelected.bind(this, classval);
                return <tr onClick={boundClick} className={(classval._id === this.state.selectedClass._id)  ? "selected" : ""}>
                        <td>{classval.name}</td>
                    </tr>;
            }.bind(this));
        }
    },
    render: function () {
        var studentBody = <tbody> {this.studentRows()} </tbody>;
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
            {studentBody}
                    </table>
                </div>
            </div>
        </div>;
    }
});

module.exports = exports;
