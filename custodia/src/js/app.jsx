var React = require('react'),
    Router = require('react-router'),
    routerContainer = require('./routercontainer'),
    Nav = require('./nav.jsx'),
    Student = require('./student.jsx'),
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
            <RouteHandler />
        </div>
    );
  }
});

var routes = (
  <Route path="/" handler={App}>
    <Route name="students" path="students" handler={StudentTable}/>
    <Route name="student" path="students/:studentId" handler={Student}/>
    <DefaultRoute handler={StudentTable}/>
  </Route>
);

router = Router.create(routes);
routerContainer.set(router);
router.run(function (Handler) {
  React.render(<Handler />, document.body);
});
