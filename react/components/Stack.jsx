import React from 'react';
import { Stack as MuiStack } from '@mui/material';
import { combineSx } from '../utils';

const stackVariants = {
  section: {
    sx: { mt: 1 },
  },
  modalHeader: {
    props: {
      direction: 'row',
      alignItems: 'flex-start',
      justifyContent: 'space-between',
    },
    spacing: 2,
    sx: { mt: 0 },
  },
  modalFooter: {
    props: {
      direction: 'row',
      justifyContent: 'flex-end',
      alignItems: 'center',
    },
    spacing: 1.5,
    sx: { mt: 0 },
  },
  row: {
    props: {
      direction: 'row',
      alignItems: 'center',
    },
    sx: { mt: 0 },
  },
};

const normalizeVariant = (variant) => {
  if (!variant) {
    return [];
  }
  return Array.isArray(variant) ? variant : [variant];
};

const Stack = React.forwardRef(({ sx, spacing, variant, ...props }, ref) => {
  const variantKeys = normalizeVariant(variant);
  const variantConfigs = variantKeys
    .map((key) => stackVariants[key])
    .filter(Boolean);

  const variantProps = variantConfigs.reduce((acc, config) => {
    if (config?.props) {
      Object.assign(acc, config.props);
    }
    return acc;
  }, {});

  const variantSpacing = variantConfigs.reduce(
    (value, config) => (config?.spacing !== undefined ? config.spacing : value),
    undefined,
  );

  const variantSx = variantConfigs.map((config) => config?.sx).filter(Boolean);

  const resolvedSpacing = spacing !== undefined ? spacing : variantSpacing ?? 2;

  return (
    <MuiStack
      ref={ref}
      spacing={resolvedSpacing}
      {...variantProps}
      {...props}
      sx={combineSx({ mt: 0 }, ...variantSx, sx)}
    />
  );
});

Stack.displayName = 'Stack';

export default Stack;
