var React = require('react'),
  Modal = require('./modal.jsx'),
  actionCreator = require('./studentactioncreator'),
  studentStore = require('./StudentStore'),
  Router = require('react-router'),
  Link = Router.Link,
  userStore = require('./userstore'),
  SuperItem = require('./superwrapper.jsx');

var exports = React.createClass({
    contextTypes: {
        router: React.PropTypes.func
    },
    getInitialState: function () {
        return {schemas: userStore.getSchemas(),
                selectedSchema: userStore.getSuperSchema()};
    },
    componentDidMount: function () {
        userStore.addChangeListener(this._onChange);
        //this.setState({student: studentStore.getStudent(this.state.studentId)});
    },
    componentWillUnmount: function () {
        userStore.removeChangeListener(this._onChange);
    },
    selectSchema: function(s) {
        userStore.setSuperSchema(s);
        this.setState({selectedSchema:s});
    },
    makeItems: function() {
        var that = this;
        return this.state.schemas.map(function(schema){
            return (<li><a onClick={that.selectSchema.bind(that,schema)}>
                     {schema}</a></li>);
        });
    },
    makeDropDown: function() {
        return (<div className="dropdown">
            <button className="btn btn-default dropdown-toggle"
            type="button" id="dropdownMenu1"
            data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
            {this.state.selectedSchema}
                <span className="caret"></span>
            </button>

            <ul className="dropdown-menu" aria-labelledby="dropdownMenu1">
            {this.makeItems()}
        </ul>
      </div>);
    },
    makeStudent: function() {
        actionCreator.makeStudent(this.state.username, this.state.password);
        this.setState({username: "", password:""});
    },
    handleUsernameChange: function(event) {
        this.setState({username: event.target.value});
    },
    handlePasswordChange: function(event) {
        this.setState({password: event.target.value});
    },
    render: function () {
        return <SuperItem>
            <div>Administration
                {this.makeDropDown()}
                <div>
                    <h2>Make Student</h2>
                    <form className="navbar-form navbar-left" role="search">
                        <div className="row">
                            <div className="col-lg-6">
                                <div className="input-group">
                                    <input type="text"
                                           className="form-control"
                                           placeholder="Username"
                                           value={this.state.username}
                                           onChange={this.handleUsernameChange}
                                           aria-describedby="username">
                                    </input>
                                </div>
                            </div>
                            <div className="col-lg-6">
                                <div className="input-group">
                                    <input type="text"
                                           className="form-control"
                                           value={this.state.password}
                                           onChange={this.handlePasswordChange}
                                           placeholder="Password"
                                           aria-describedby="password">
                                    </input>
                                    <button type="button"
                                            onClick={this.makeStudent.bind(this)}
                                            className="btn btn-default"
                                            aria-label="Left Align">
                                        <span className="glyphicon glyphicon-save" aria-hidden="true"></span>
                                        Create
                                    </button>
                                </div>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
            </SuperItem>
    },
    _onChange: function () {
        this.setState({schemas: userStore.getSchemas(),
                       selectedSchema: userStore.getSuperSchema()});
    }
});

module.exports = exports;
