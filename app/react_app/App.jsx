import React, { useState } from "react";
import { ThemeProvider } from "@mui/material/styles";
import { Snackbar, Alert } from "@mui/material";
import { SnackbarContext, DefaultSnackbar } from './contexts';
import { lightTheme } from "./theme/theme";
import Navigation from "./containers/Navigation.jsx";

import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';

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
        <Navigation />
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
