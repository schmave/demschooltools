import React from "react";
import ReactDOM from "react-dom/client";
import "./edit_minutes.scss";
import Case from "./minutes/Case";

function RootApp() {
  return (
    <React.StrictMode>
      <>
        <div className="div-test">Hello World, No More Classes</div>

        {window.initialData.cases.map((caseData) => (
          <Case key={caseData.label} caseData={caseData} />
        ))}
      </>
    </React.StrictMode>
  );
}

const root = ReactDOM.createRoot(document.getElementById("react-root"));
root.render(<RootApp />);
