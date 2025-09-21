import React from 'react';
import TextField from '../TextField/TextField';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { DesktopDatePicker } from '@mui/x-date-pickers/DesktopDatePicker';

const DatePicker = (props) => {
  const { value, setValue, fullWidth = true, inputFormat = 'MM/DD/YYYY', ...restOfProps } = props;

  const handleChange = (newValue) => {
    setValue(newValue);
  };

  return (
    <LocalizationProvider dateAdapter={AdapterDayjs}>
      <DesktopDatePicker
        inputFormat={inputFormat}
        value={value}
        onChange={handleChange}
        fullWidth={fullWidth}
        renderInput={(params) => <TextField {...params} />}
        {...restOfProps}
      />
    </LocalizationProvider>
  );
}

export default DatePicker;
