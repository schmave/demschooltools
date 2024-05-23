import Globalize from "globalize";
import React, { Component } from "react";
import { render as _render } from "react-dom";
import { IndexRoute, Route, Router } from "react-router";
import globalizeLocalizer from "react-widgets-globalize";

import Flash from "./flashnotification.jsx";
import myhistory from "./myhistory.js";
import Nav from "./nav.jsx";
import Student from "./student.jsx";
import StudentReports from "./studentreports.jsx";
import StudentTable from "./studenttable.jsx";

Globalize.load(
  require("cldr-data/main/en/ca-gregorian"),
  require("cldr-data/main/en/numbers"),
  require("cldr-data/supplemental/likelySubtags"),
  require("cldr-data/supplemental/timeData"),
  require("cldr-data/supplemental/weekData"),
);

Globalize.locale("en");
globalizeLocalizer();

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
