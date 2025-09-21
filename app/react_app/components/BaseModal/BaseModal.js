import React from 'react';
import { Box, Modal } from '@mui/material';

const modalContentStyle = {
  position: 'absolute',
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  bgcolor: 'background.paper',
  border: '2px solid #0c62fb',
  boxShadow: 24,
  borderRadius: 5,
  p: 4,
  maxHeight: '80vh',
  minWidth: '30vw',
  overflowY: 'scroll',
};

const BaseModal = (props) => {
  const { open, handleClose, children, ...restOfProps } = props;

  return (
    <Modal
      open={open}
      onClose={handleClose}
      {...restOfProps}
    >
      <Box sx={modalContentStyle}>
        {children}
      </Box>
    </Modal>
  );
}

export default BaseModal;
