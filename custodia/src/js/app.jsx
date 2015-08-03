var React = require('react'),
    Router = require('react-router'),
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
    <Route path="students" handler={StudentTable}/>
    <Route name="students" path="students/:studentId" handler={Student}/>
    <DefaultRoute handler={StudentTable}/>
  </Route>
);

Router.run(routes, function (Handler) {
  React.render(<Handler />, document.body);
});

//React.render(<Nav />, document.getElementById('nav'));
//React.render(<StudentTable />, document.getElementById('content'));