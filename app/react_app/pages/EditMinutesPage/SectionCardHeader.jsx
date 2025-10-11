import React from 'react';
import { Box, Typography } from '../../components';

const SectionCardHeader = ({ title, action }) => (
  <Box
    sx={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: action ? 'space-between' : 'center',
      px: 2,
      pt: 2,
      pb: 0,
    }}
  >
    <Typography
      variant="h5"
      sx={{
        fontWeight: 'bold',
        textAlign: action ? 'left' : 'center',
        width: action ? 'auto' : '100%',
      }}
    >
      {title}
    </Typography>
    {action ? <Box sx={{ ml: 2, flexShrink: 0 }}>{action}</Box> : null}
  </Box>
);

export default SectionCardHeader;
