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
        return {schemas: userStore.getSchemas()};
    },
    componentDidMount: function () {
        userStore.addChangeListener(this._onChange);
        //this.setState({student: studentStore.getStudent(this.state.studentId)});
    },
    componentWillUnmount: function () {
        userStore.removeChangeListener(this._onChange);
    },
    makeItems: function() {
        return this.state.schemas.map(function(schema){
            return (<li><a href="#">{schema}</a></li>);
        });
    },
    makeDropDown: function() {
        return (<div className="dropdown">
            <button className="btn btn-default dropdown-toggle"
            type="button" id="dropdownMenu1"
            data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
            Dropdown
                <span className="caret"></span>
            </button>

            <ul className="dropdown-menu" aria-labelledby="dropdownMenu1">
            {this.makeItems()}
        </ul>
      </div>);
    },
    render: function () {
        return <SuperItem>
            <div>Administration
            {this.makeDropDown()}
            </div>
            </SuperItem>
    },
    _onChange: function () {
        this.setState({schemas: userStore.getSchemas()});
    }
});

module.exports = exports;
