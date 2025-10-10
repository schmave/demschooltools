import React, { useState } from "react";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { ThemeProvider } from "@mui/material/styles";
import { Snackbar, Alert } from "@mui/material";
import { SnackbarContext, DefaultSnackbar } from './contexts';
import { lightTheme } from "./theme/theme";

import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';

// And Pages:
import {
  EditMinutesPage,
  ReadMinutesPage
} from './pages';

// We're not controlling routing with React, but this lets us use one React App and map the Play/Scala paths to React Pages
const router = createBrowserRouter([
  { path: "/editTodayReact", element: <EditMinutesPage /> },
  { path: "/readTodayReact", element: <ReadMinutesPage /> },
]);

function App() {
  const [showSnackbar, setShowSnackbar] = useState(false);
  const [snackbarDetails, setSnackbarDetails] = useState(DefaultSnackbar);

  // Pieces for the Snackbar Context
  const setSnackbar = (props) => {
    const { message, action, severity = "success", duration = 5000 } = props;
    setSnackbarDetails({
      message,
      duration,
      action,
      severity,
    });
    setShowSnackbar(true);
  };

  const onDismissSnackBar = () => {
    setShowSnackbar(false);
    setSnackbarDetails(DefaultSnackbar);
  };

  return (
    <ThemeProvider theme={lightTheme}>
      <SnackbarContext.Provider
        value={{ snackbar: snackbarDetails, setSnackbar }}
      >
        <RouterProvider router={router} />
        <Snackbar
          open={showSnackbar}
          onClose={onDismissSnackBar}
          message={snackbarDetails.message}
          action={snackbarDetails.action}
          autoHideDuration={snackbarDetails.duration}
        >
          <Alert
            onClose={onDismissSnackBar}
            severity={snackbarDetails.severity}
            sx={{ width: "100%" }}
          >
            {snackbarDetails.message}
          </Alert>
        </Snackbar>
      </SnackbarContext.Provider>
    </ThemeProvider>
  );
}

export default App;
