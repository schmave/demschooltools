import { createTheme, responsiveFontSizes } from '@mui/material/styles';

// TO-DO: Replace colors here to match the rest of the app
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
  typography: {
    fontSize: 20,
  },
});

let lightTheme = createTheme({
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
  typography: {
    fontSize: 20,
  },
});
lightTheme = responsiveFontSizes(lightTheme);
export { lightTheme };

