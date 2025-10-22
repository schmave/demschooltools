import { Component } from "react";
import { createRoot } from "react-dom/client";
import { HashRouter, Outlet, Route, Routes } from "react-router-dom";

import "../css/starter-template.css";
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
        <div className="content">
          <Outlet />
        </div>
      </div>
    );
  }
}

const root = document.getElementById("react_container");

createRoot(root).render(
  <HashRouter>
    <Routes>
      <Route element={<App />}>
        <Route index element={<StudentTable />} />
        <Route path="students" element={<StudentTable />} />
        <Route path="students/:studentId/:day?" element={<Student />} />
        <Route path="reports" element={<StudentReports />} />
      </Route>
    </Routes>
  </HashRouter>,
);
