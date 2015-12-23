var React = require('react'),
    actionCreator = require('./studentactioncreator');

var exports = React.createClass({
    submit: function(){
        actionCreator.createStudent(this.refs.name.getDOMNode().value);
    },
    render: function () {
        return <div className="row">
            <div className="col-sm-4"></div>
            <div className="col-sm-4">
                <div className="panel panel-success">
                    <div className="panel-heading">
                        <h3 className="panel-title">Add Student</h3>
                    </div>
                    <div className="panel-body">
                        <form>
                            <div className="form-group">
                                <label htmlFor="studentName">Name</label>
                                <input ref="name" className="form-control" id="studentName" placeholder="Name"/>
                            </div>
                            <div className="form-group">
                                <button type="button" id="saveStudent" onClick={this.submit} className="btn btn-primary">Add Student</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>;
    }
});

module.exports = exports;

//var UncontrolledInput = React.createClass({
//  reset: function() {
//    this.refs.input.getDOMNode().value = "Hello!";
//  },
//
//  alertValue: function() {
//    alert(this.refs.input.getDOMNode().value);
//  },
//
//  render: function() {
//    return (
//      <div>
//        <input ref="input" defaultValue="Hello!" />
//        <button onClick={this.reset}>Reset</button>
//        <button onClick={this.alertValue}>Alert Value</button>
//      </div>
//    );
//  }
//});
