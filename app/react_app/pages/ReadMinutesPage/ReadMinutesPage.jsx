import React, { useState, useEffect } from "react";
import { Typography } from "../../components";

const ReadMinutesPage = () => {
  const [cases, setCases] = useState(window.initialData.cases);
  const [people, setPeople] = useState(window.initialData.people);

  return (
    <>
      <div className="div-test"><Typography>Read Minutes View, AKA a Second Page</Typography></div>
      {cases.map((caseData) => (
        <div key={caseData.label}><Typography>I am case #{caseData.label}</Typography></div>
      ))}
    </>
  );
};

export default ReadMinutesPage;
