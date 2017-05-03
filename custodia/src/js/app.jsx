var React = require('react'),
    Router = require('react-router'),
    routerContainer = require('./routercontainer'),
    Nav = require('./nav.jsx'),
    Flash = require('./flashnotification.jsx'),
    Student = require('./student.jsx'),
    StudentAdmin = require('./studentAdmin.jsx'),
    CreateStudent = require('./createstudent.jsx'),
    CreateAClass = require('./createaclass.jsx'),
    Administration = require('./administration.jsx'),
    SwipeListing = require('./swipeslisting.jsx'),
    StudentReports = require('./studentreports.jsx'),
    Classes = require('./classes.jsx'),
    StudentTable = require('./studenttable.jsx');


var DefaultRoute = Router.DefaultRoute;
var Link = Router.Link;
var Route = Router.Route;
var RouteHandler = Router.RouteHandler;

var App = React.createClass({
    render: function () {
        return (
            <div>
                <Nav />
                <Flash />
                <div className="content">
                    <RouteHandler />
                </div>
            </div>
        );
    }
});

var routes = (
    <Route path="/" handler={App}>
        <Route name="students" path="students" handler={StudentTable}/>
        <Route name="create" path="students/new" handler={CreateStudent}/>
        <Route name="student" path="students/:studentId/:day?" handler={Student} />
        <Route name="studentAdmin" path="studentAdmin" handler={StudentAdmin} />
        <Route name="reports" path="reports" handler={StudentReports} />
        <Route name="classes" path="classes" handler={Classes} />
        <Route name="createaclass" path="class/new" handler={CreateAClass} />
        <Route name="admin" path="administration" handler={Administration} />

        <DefaultRoute handler={StudentTable}/>
    </Route>
);

var router = Router.create(routes);
routerContainer.set(router);
router.run(function (Handler) {
    React.render(<Handler />, document.body);
});
