import { createTheme } from '@mui/material/styles';

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
      // main: '#c7b9ff', // Slalom Purple, replaced with a gray below
      main: '#7e8289',
    },
    error: {
      main: '#ff4d5f',
    },
    warning: {
      main: '#ffcc00',
      // main: '#deff4d', // Slalom color, but it's terrible
    },
  },
});