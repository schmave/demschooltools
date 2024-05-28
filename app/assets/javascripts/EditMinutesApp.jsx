import React, { useState, useEffect } from "react";
import ReactDOM from "react-dom/client";
import "./edit_minutes.scss";
import Case from "./minutes/Case";

function RootApp() {
  const [cases, setCases] = useState(window.initialData.cases);
  const [chair, setChair] = useState(window.initialData.chair);
  const [committee, setCommittee] = useState(window.initialData.committee);
  const [config, setConfig] = useState(window.initialData.config);
  const [meetingId, setMeetingId] = useState(window.initialData.meeting_id);
  const [noteTaker, setNoteTaker] = useState(window.initialData.notetaker);
  const [people, setPeople] = useState(window.initialData.people);
  const [rules, setRules] = useState(window.initialData.rules);
  const [runners, setRunners] = useState(window.initialData.runners);
  const [sub, setSub] = useState(window.initialData.sub);
  
  return (
    <React.StrictMode>
      <>
        <div className="div-test">Hello World, No More Classes</div>

        {window.initialData.cases.map((caseData) => (
          <Case key={caseData.label} caseData={caseData} />
        ))}
        {people.map((person) => (
          <div key={person.id}>{person.label}</div>
        ))}
      </>
    </React.StrictMode>
  );
}

const root = ReactDOM.createRoot(document.getElementById("react-root"));
root.render(<RootApp />);
