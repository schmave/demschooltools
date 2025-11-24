import * as React from "react";

export const DefaultSnackbar = {
  message: '',
  duration: 7000,
  action: undefined,
  severity: 'success'
}

export const SnackbarContext = React.createContext({
  DefaultSnackbar
});
