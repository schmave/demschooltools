import React from 'react';
import { Button as MuiButton } from '@mui/material';

const Button = (props) => {
  const { variant = 'outlined', ...restOfProps } = props;
  return (
    <MuiButton variant={variant} {...restOfProps} />
  );
};

export default Button;
