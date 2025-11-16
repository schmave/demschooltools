import React from 'react';
import { TextField as MuiTextField } from '@mui/material';

const TextField = (props) => {
  const { variant = 'outlined', ...restOfProps } = props;
  return (
    <MuiTextField variant={variant} {...restOfProps} />
  );
};

export default TextField;
