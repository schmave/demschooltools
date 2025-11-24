import React from 'react';
import Box from './Box';

const PageWrapper = React.forwardRef(({ sx, surface, variant, ...props }, ref) => {
  return (
    <Box
      ref={ref}
      variant={variant}
      sx={{
        px: 0,
        pt: { xs: 1, md: 1.5 },
        pb: { xs: 2, md: 3 },
        minHeight: '100%',
        ...sx,
      }}
      {...props}
    />
  );
});

PageWrapper.displayName = 'PageWrapper';

export default PageWrapper;
