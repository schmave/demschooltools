import React from 'react';
import { CardHeader as MuiCardHeader } from '@mui/material';

const CardHeader = React.forwardRef(({ sx, ...props }, ref) => {
  return (
    <MuiCardHeader
      ref={ref}
      sx={{
        '&.MuiCardHeader-root': {
          paddingBottom: (theme) => theme.spacing(1.5),
        },
        ...sx,
      }}
      {...props}
    />
  );
});

CardHeader.displayName = 'CardHeader';

export default CardHeader;
