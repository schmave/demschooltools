import React from "react";
import CustodiaShell from "./app.jsx";
import Student from "./student.jsx";
import StudentReports from "./studentreports.jsx";
import StudentTable from "./studenttable.jsx";
import withRouter from "./withRouter.jsx";

const StudentWithRouter = withRouter(Student);

export const custodiaRoute = {
  path: "/custodia",
  element: <CustodiaShell />,
  children: [
    { index: true, element: <StudentTable /> },
    { path: "students", element: <StudentTable /> },
    { path: "students/:studentId", element: <StudentWithRouter /> },
    { path: "students/:studentId/:day", element: <StudentWithRouter /> },
    { path: "reports", element: <StudentReports /> },
  ],
};
