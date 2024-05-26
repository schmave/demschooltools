import React from "react";
import { createRoot } from "react-dom/client";
import "./edit_minutes.scss";
import Case from "./minutes/Case";

export class EditMinutesApp extends React.Component {
  render = () => (
    <>
      <div className="div-test">Hello, world!</div>

      {window.initialData.cases.map((caseData) => (
        <Case key={caseData.label} caseData={caseData} />
      ))}
    </>
  );
}

const root = createRoot(document.getElementById("react-root"));
root.render(<EditMinutesApp />);
