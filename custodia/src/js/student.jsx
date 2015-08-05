var React = require('react'),
    actionCreator = require('./studentactioncreator'),
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
        this.setState({student: studentStore.getStudent(this.state.studentId)});
    },
    componentWillUnmount: function () {
        studentStore.removeChangeListener(this._onChange);
    },
    signIn: function () {
        actionCreator.swipeStudent(this.state.student, 'in');
    },
    signOut: function () {
        actionCreator.swipeStudent(this.state.student, 'out');
    },
    markAbsent: function () {
        actionCreator.markAbsent(this.state.student);
    },
    getActionButtons: function(){
        var buttons = [];

        if(this.state.student.last_swipe_date !== this.state.student.today || this.state.student.last_swipe_type === 'out'){
            buttons.push(<button type="button" onClick={this.signIn}
                                 className="btn btn-sm btn-info margined">Sign In
            </button>);
        }
        if(this.state.student.last_swipe_date !== this.state.student.today || this.state.student.last_swipe_type === 'in'){
            buttons.push(<button type="button" onClick={this.signOut}
                                 className="btn btn-sm btn-info margined">Sign Out
            </button>);
        }
        if(!this.state.student.absent_today){
            buttons.push(<button type="button" onClick={this.markAbsent}
                                 className="btn btn-sm btn-info margined">Absent
            </button>);
        }

        return buttons;
    },
    todaysSwipes: function(){
        var swipes = [];
        this.state.student.days[0].swipes.map(function(swipe){
            swipes.push(<tr><td>{swipe.nice_in_time}</td><td>{swipe.nice_out_time}</td><td></td></tr>)
        })
        return swipes;
    },
    render: function () {
        if (this.state.student) {
            return <div className="content row">
                <div className="col-sm-1"></div>
                <div className="col-sm-10">
                    <div className="panel panel-info">
                        <div className="panel-heading">
                            <div className="row">
                                <div className="col-sm-8">
                                    <h1 className="pull-left">{this.state.student.name} </h1>
                                    <h2 className="badge badge-red">{this.state.student.absent_today ? 'Absent' : ''}</h2>
                                </div>
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
                                <div className="col-sm-7">
                                    <div className="col-sm-5">
                                        {this.getActionButtons()}
                                    </div>
                                </div>
                                <div className="col-sm-5">
                                    <table className="table table-striped">
                                        <thead>
                                            <tr>
                                                <th>In Time</th>
                                                <th>Out Time</th>
                                                <th>Minutes</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {this.todaysSwipes()}
                                        </tbody>
                                    </table>
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