import React, { Component } from "react";
import { render as _render } from "react-dom";
import { Route } from "react-router";
import { HashRouter } from "react-router";
import { Routes } from "react-router";

import Flash from "./flashnotification.jsx";
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

const root = document.getElementById("react_container");

ReactDOM.createRoot(root).render(
  <HashRouter>
    <Routes>
      <Route path="/" component={App} />
      <Route path="students" component={StudentTable} />
      <Route path="students/:studentId(/:day)" component={Student} />
      <Route path="reports" component={StudentReports} />

      <Route index component={StudentTable} />
    </Routes>
  </HashRouter>,
);
