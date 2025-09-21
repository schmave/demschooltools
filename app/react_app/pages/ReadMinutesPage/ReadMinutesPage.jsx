import React, { useState, useEffect, useContext } from "react";
import { Typography, Button } from "../../components";
import { SnackbarContext } from '../../contexts';

const ReadMinutesPage = () => {
  const [cases, setCases] = useState(window.initialData.cases);
  const [people, setPeople] = useState(window.initialData.people);
  const { setSnackbar } = useContext(SnackbarContext);

  const onButtonClick = () => {
    setSnackbar({ message: 'Look at this awesome Snackbar!' })
  }

  return (
    <>
      <div className="div-test"><Typography>Read Minutes View, AKA a Second Page,, new changes!</Typography></div>
      {cases.map((caseData) => (
        <div key={caseData.label}><Typography>I am case #{caseData.label}</Typography></div>
      ))}
      <Button onClick={onButtonClick}>Click Me for a Snackbar!</Button>
    </>
  );
};

export default ReadMinutesPage;
