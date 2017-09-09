var React = require('react'),
    actionCreator = require('./classactioncreator');

var exports = React.createClass({
    submit: function(){
        actionCreator.createClass(this.refs.name.getDOMNode().value);
    },

    render: function () {
        var 
        return <div className="row">
            <div className="col-sm-4"></div>
            <div className="col-sm-4">
                <div className="panel panel-success">
                    <div className="panel-heading">
                    </div>
                    <div className="panel-body">
                        <form>
                            <div className="form-group">
                                <label htmlFor="Class">Name</label>
                                <input ref="name" className="form-control" id="Class" placeholder="Name"/>
                            </div>
                            <div className="form-group">
                                <button id="create-class-button" type="button" onClick={this.submit} className="btn btn-primary">Add Class</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>;
    }
});

module.exports = exports;
