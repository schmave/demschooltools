import { createTheme } from '@mui/material/styles';

// TO-DO: Replace colors here, pulled from a Slalom app
export const darkTheme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#0c62fb',
    },
    secondary: {
      main: '#1be1f2',
    },
    info: {
      main: '#c7b9ff',
    },
    error: {
      main: '#ff4d5f',
    },
    warning: {
      main: '#deff4d',
    },
  },
});

export const lightTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#0c62fb',
    },
    secondary: {
      main: '#1be1f2',
    },
    info: {
      main: '#7e8289',
    },
    error: {
      main: '#ff4d5f',
    },
    warning: {
      main: '#ffcc00',
    },
  },
});
