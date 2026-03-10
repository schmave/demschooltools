import React, { useState } from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import { Snackbar, Alert } from '@mui/material';
import { SnackbarContext, DefaultSnackbar } from './contexts';
import { lightTheme } from './theme/theme';

import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';

import SignInSheetPage from './pages/SignInSheetPage/SignInSheetPage';
import CustomFieldsSettingsPage from './pages/CustomFieldsSettingsPage/CustomFieldsSettingsPage';
import PersonFormPage from './pages/PersonPage/PersonFormPage';
import PersonViewPage from './pages/PersonPage/PersonViewPage';
import PeopleListPage from './pages/PeopleListPage/PeopleListPage';

// We're not controlling routing with React, but this lets us use one React App
// and map the Play/Scala paths to React Pages
const router = createBrowserRouter([
    { path: '/people/list', element: <PeopleListPage /> },
    { path: '/attendance/signInSheet', element: <SignInSheetPage /> },
    { path: '/settings/custom-fields', element: <CustomFieldsSettingsPage /> },
    { path: '/people/new', element: <PersonFormPage /> },
    { path: '/people/edit/:id', element: <PersonFormPage /> },
    { path: '/people/:id', element: <PersonViewPage /> },
]);

function App() {
    const [showSnackbar, setShowSnackbar] = useState(false);
    const [snackbarDetails, setSnackbarDetails] = useState(DefaultSnackbar);

    // Pieces for the Snackbar Context
    const setSnackbar = (props) => {
        const {
            message,
            action,
            severity = 'success',
            duration = 5000,
        } = props;
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
                        sx={{ width: '100%' }}
                    >
                        {snackbarDetails.message}
                    </Alert>
                </Snackbar>
            </SnackbarContext.Provider>
        </ThemeProvider>
    );
}

export default App;
