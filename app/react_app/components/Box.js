import React from 'react';
import { Box as MuiBox } from '@mui/material';

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

const Box = React.forwardRef(({ sx, surface = false, ...props }, ref) => {
  const resolvedSx = surface ? mergeSurfaceSx(sx) : sx;
  return <MuiBox ref={ref} sx={resolvedSx} {...props} />;
});

Box.displayName = 'Box';

export default Box;
