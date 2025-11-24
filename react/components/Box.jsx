import React from 'react';
import { Box as MuiBox } from '@mui/material';
import { combineSx } from '../utils';

const baseSurfaceSx = {
  border: '1px solid',
  borderColor: 'divider',
  borderRadius: 2,
  boxShadow: '0px 12px 24px rgba(15, 30, 60, 0.08)',
};

const boxVariants = {
  surface: baseSurfaceSx,
  modalContent: {
    position: 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: { xs: 'calc(100% - 32px)', sm: 'min(720px, 90vw)' },
    maxHeight: '85vh',
    bgcolor: 'background.paper',
    borderRadius: 3,
    boxShadow: 24,
    outline: 'none',
    overflow: 'hidden',
    display: 'flex',
    flexDirection: 'column',
  },
  modalBody: {
    flex: '1 1 auto',
    overflowY: 'auto',
    minHeight: 0,
  },
  richText: {
    '& table': { width: '100%', borderCollapse: 'collapse' },
    '& th, & td': {
      padding: 1,
      borderBottom: '1px solid',
      borderColor: 'divider',
    },
  },
};

const normalizeVariant = (variant, surface) => {
  const list = [];
  if (surface) {
    list.push('surface');
  }
  if (!variant) {
    return list;
  }
  if (Array.isArray(variant)) {
    list.push(...variant);
  } else {
    list.push(variant);
  }
  return list;
};

const Box = React.forwardRef(({ sx, surface = false, variant, ...props }, ref) => {
  const variantKeys = normalizeVariant(variant, surface);
  const variantStyles = variantKeys.map((key) => boxVariants[key]).filter(Boolean);
  const resolvedSx = combineSx(...variantStyles, sx);

  return <MuiBox ref={ref} sx={resolvedSx} {...props} />;
});

Box.displayName = 'Box';

export default Box;
