import React, { useState, useEffect } from "react";
import { Typography } from "../../components";

const EditMinutesPage = () => {
  const [people, setPeople] = useState(window.initialData.people);

  return (
    <>
      <div className="div-test"><Typography>Edit Minutes View, Now in React Folder, React Structure, changed, less</Typography></div>
      {people.map((person) => (
        <div key={person.id}><Typography>{person.label}</Typography></div>
      ))}
    </>
  );
};

export default EditMinutesPage;
