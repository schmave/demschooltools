import React from 'react';
import { Paper as MuiPaper } from '@mui/material';

const baseSurfaceSx = {
  border: '1px solid',
  borderColor: 'divider',
  borderRadius: 2,
  boxShadow: '0px 12px 24px rgba(15, 30, 60, 0.08)',
};

const mergeSurfaceSx = (sx) => {
  if (!sx) {
    return baseSurfaceSx;
  }
  if (Array.isArray(sx)) {
    return [baseSurfaceSx, ...sx];
  }
  return [baseSurfaceSx, sx];
};

const Paper = React.forwardRef(({ sx, elevation = 0, ...props }, ref) => {
  return <MuiPaper ref={ref} elevation={elevation} sx={mergeSurfaceSx(sx)} {...props} />;
});

Paper.displayName = 'Paper';

export default Paper;
