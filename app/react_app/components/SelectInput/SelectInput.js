import React from 'react';
import { InputLabel, MenuItem, FormControl, Select, useTheme } from '@mui/material';
import {
  CgClose
} from 'react-icons/cg';
import Divider from '../Divider/Divider';
import Typography from '../Typography/Typography';

const SelectInput = (props) => {
  const { value, setValue, label, fullWidth = true, options, disabled, marginTop = '10px', marginBottom = '10px', showOptionCount = false, showClearButton = false, clearButtonLabel = 'Clear Selection', ...restOfProps } = props;
  const theme = useTheme();

  const handleChange = (event) => {
    setValue(event.target.value);
  };

  return (
      <FormControl fullWidth={fullWidth} sx={{ marginTop, marginBottom }}>
        {label && (
          <InputLabel id="select-label">{label}</InputLabel>
        )}
        <Select
          value={value}
          label={label}
          onChange={handleChange}
          disabled={disabled || options.length === 0}
          {...restOfProps}
        >
          {showClearButton && (
            <MenuItem key={'clearSelectionOverride'} value={'clearSelectionOverride'} sx={{ justifyContent: 'space-between' }}>
              {clearButtonLabel}
              <CgClose style={{ marginRight: '5px' }} color={theme.palette.primary.main} />
            </MenuItem>
          )}
          {showClearButton && (
            <Divider />
          )}
          {options.map((option) => {
            return (
              <MenuItem key={option.value} value={option.value} sx={{ color: (showOptionCount && option.count === 0) ? theme.palette.text.disabled : 'auto' }}>
                {option.leftIcon && (
                  option.leftIcon
                )}
                {option.label}
                {showOptionCount && (` (${option.count})`)}
                {option.rightIcon && (
                  option.rightIcon
                )}
              </MenuItem>
            )
          })}
        </Select>
      </FormControl>
  );
}

export default SelectInput;
