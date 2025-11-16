import React from 'react';
import { CardContent as MuiCardContent } from '@mui/material';

const baseSx = {
  '&:last-child': {
    paddingBottom: undefined,
  },
};

const CardContent = React.forwardRef(({ sx, ...props }, ref) => {
  return <MuiCardContent ref={ref} sx={{ ...baseSx, ...sx }} {...props} />;
});

CardContent.displayName = 'CardContent';

export default CardContent;
