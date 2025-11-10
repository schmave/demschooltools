import React from "react";
import { Outlet } from "react-router-dom";
import Flash from "./flashnotification.jsx";
import Nav from "./nav.jsx";

const CustodiaShell = () => (
  <div>
    <Nav />
    <Flash />
    <div className="content">
      <Outlet />
    </div>
  </div>
);

export default CustodiaShell;
