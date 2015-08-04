var React = require('react'),
    studentStore = require('./StudentStore');

var exports = React.createClass({
    contextTypes: {
        router: React.PropTypes.func
    },
    getInitialState: function () {
        return {studentId: this.context.router.getCurrentParams().studentId};
    },
    componentDidMount: function () {
        studentStore.addChangeListener(this._onChange)
        studentStore.getStudent(this.state.studentId);
    },
    componentWillUnmount: function(){
        studentStore.removeChangeListener(this._onChange);
    },
    render: function () {
        if (this.state.student) {
            return <div className="content row">
                <div className="col-sm-1"></div>
                <div className="col-sm-10">
                    <div className="panel panel-info">
                        <div className="panel-heading">
                            <div className="row">
                                <div className="col-sm-8"><h1>{this.state.student.name}</h1></div>
                                <div className="col-sm-4">
                                    <div className="col-sm-6"><b>Attended:</b> {this.state.student.total_days}</div>
                                    <div className="col-sm-6"><b>Absent:</b> {this.state.student.total_abs}</div>
                                    <div className="col-sm-6"><b>Excused:</b> {this.state.student.total_excused}</div>
                                    <div className="col-sm-6"><b>Overrides:</b> {this.state.student.total_overrides}
                                    </div>
                                    <div className="col-sm-6"><b>Short:</b> {this.state.student.total_short}</div>
                                </div>
                            </div>
                        </div>
                        <div className="panel-body">
                            <div className="row">
                                <div className="col-sm-3">
                                    <button type="button" className="btn btn-sm btn-info margined">Sign In</button>
                                    <button type="button" className="btn btn-sm btn-info margined">Sign Out</button>
                                    <button type="button" className="btn btn-sm btn-info margined">Absent</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="col-sm-1"></div>
            </div>;
        }

        return <div></div>
            ;
    },
    _onChange: function () {
        this.setState({
            student: studentStore.getStudent(this.state.studentId),
            studentId: this.state.studentId
        })
    }
});

module.exports = exports;