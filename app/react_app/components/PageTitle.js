import React from 'react';
import Typography from './Typography';

const PageTitle = React.forwardRef(({ children, sx, ...props }, ref) => {
  return (
    <Typography
      ref={ref}
      variant="h3"
      sx={{ fontWeight: 600, ...sx }}
      {...props}
    >
      {children}
    </Typography>
  );
});

PageTitle.displayName = 'PageTitle';

export default PageTitle;
