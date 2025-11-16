import React from 'react';
import { Paper as MuiPaper } from '@mui/material';
import { combineSx } from '../utils';

const baseSurfaceSx = {
  border: '1px solid',
  borderColor: 'divider',
  borderRadius: 2,
  boxShadow: '0px 12px 24px rgba(15, 30, 60, 0.08)',
  p: 2,
};

const paperVariants = {
  compact: {
    p: 1.5,
  },
};

const normalizeVariant = (variant) => {
  if (!variant) {
    return [];
  }
  return Array.isArray(variant) ? variant : [variant];
};

const Paper = React.forwardRef(({ sx, elevation = 0, variant, ...props }, ref) => {
  const variantKeys = normalizeVariant(variant);
  const variantStyles = variantKeys.map((key) => paperVariants[key]).filter(Boolean);
  const resolvedSx = combineSx(baseSurfaceSx, ...variantStyles, sx);

  return <MuiPaper ref={ref} elevation={elevation} sx={resolvedSx} {...props} />;
});

Paper.displayName = 'Paper';

export default Paper;
