var React = require('react'),
    ReactDOM = require('react-dom'),
    Router = require('react-router'),
    Globalize = require('globalize'),
    globalizeLocalizer = require('react-widgets-globalize'),
    Nav = require('./nav.jsx'),
    Flash = require('./flashnotification.jsx'),
    Student = require('./student.jsx'),
    StudentAdmin = require('./studentAdmin.jsx'),
    CreateAClass = require('./createaclass.jsx'),
    Administration = require('./administration.jsx'),
    SwipeListing = require('./swipeslisting.jsx'),
    StudentReports = require('./studentreports.jsx'),
    Classes = require('./classes.jsx'),
    StudentTable = require('./studenttable.jsx'),
    myhistory = require('./myhistory.js');


Globalize.load(
  require("cldr-data/main/en/ca-gregorian"),
  require("cldr-data/main/en/numbers"),
  require("cldr-data/supplemental/likelySubtags"),
  require("cldr-data/supplemental/timeData"),
  require("cldr-data/supplemental/weekData")
);

Globalize.locale('en');
globalizeLocalizer();

var App = React.createClass({
    render: function () {
        return (
            <div>
                <Nav />
                <Flash />
                <div className="content">
                    {this.props.children}
                </div>
            </div>
        );
    }
});


var Route = Router.Route;

var router = <Router.Router history={myhistory}>
        <Route path="/" component={App}>
            <Route path="students" component={StudentTable}/>
            <Route path="students/:studentId(/:day)" component={Student} />
            <Route path="studentAdmin" component={StudentAdmin} />
            <Route path="reports" component={StudentReports} />
            <Route path="classes" component={Classes} />
            <Route path="class/new" component={CreateAClass} />
            <Route path="administration" component={Administration} />

            <Router.IndexRoute component={StudentTable}/>
        </Route>
    </Router.Router>;

ReactDOM.render(router, document.getElementById('react_container'));
