var React = require('react'),
    Router = require('react-router'),
    routerContainer = require('./routercontainer'),
    Nav = require('./nav.jsx'),
    Flash = require('./flashnotification.jsx'),
    Student = require('./student.jsx'),
    CreateStudent = require('./createstudent.jsx'),
    SwipeListing = require('./swipeslisting.jsx'),
    StudentReports = require('./studentreports.jsx'),
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
        <Route name="reports" path="reports" handler={StudentReports} />
        <DefaultRoute handler={StudentTable}/>
    </Route>
);

router = Router.create(routes);
routerContainer.set(router);
router.run(function (Handler) {
    React.render(<Handler />, document.body);
});
