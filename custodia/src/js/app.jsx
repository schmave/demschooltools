import React, { Component } from "react";
import { render as _render } from "react-dom";
import { IndexRoute, Route, Router } from "react-router";

import Flash from "./flashnotification.jsx";
import myhistory from "./myhistory.js";
import Nav from "./nav.jsx";
import "./polyfill.js";
import Student from "./student.jsx";
import StudentReports from "./studentreports.jsx";
import StudentTable from "./studenttable.jsx";

class App extends Component {
  render() {
    return (
      <div>
        <Nav />
        <Flash />
        <div className="content">{this.props.children}</div>
      </div>
    );
  }
}

var router = (
  <Router history={myhistory}>
    <Route path="/" component={App}>
      <Route path="students" component={StudentTable} />
      <Route path="students/:studentId(/:day)" component={Student} />
      <Route path="reports" component={StudentReports} />

      <IndexRoute component={StudentTable} />
    </Route>
  </Router>
);

_render(router, document.getElementById("react_container"));
