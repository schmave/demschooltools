import React from 'react';
import { Card as MuiCard } from '@mui/material';

const baseSurfaceSx = {
  border: '1px solid',
  borderColor: 'divider',
  borderRadius: 3,
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

const Card = React.forwardRef(({ sx, elevation = 0, ...props }, ref) => {
  return <MuiCard ref={ref} elevation={elevation} sx={mergeSurfaceSx(sx)} {...props} />;
});

Card.displayName = 'Card';

export default Card;
