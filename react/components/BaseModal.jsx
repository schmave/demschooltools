import React from 'react';
import { Modal } from '@mui/material';
import Box from './Box';
import { combineSx } from '../utils';

const mergeContentProps = (contentProps = {}) => {
  const { sx, variant, ...rest } = contentProps;
  const variantList = Array.isArray(variant) ? variant : variant ? [variant] : [];

  return {
    ...rest,
    variant: ['modalContent', ...variantList],
    sx,
  };
};

const mergeSlotProps = (slotProps = {}) => {
  const backdrop = slotProps.backdrop || {};
  return {
    ...slotProps,
    backdrop: {
      ...backdrop,
      sx: combineSx(
        {
          bgcolor: 'rgba(15, 23, 42, 0.45)',
          backdropFilter: 'blur(2px)',
        },
        backdrop.sx,
      ),
    },
  };
};

const BaseModal = ({ open, handleClose, children, sx, contentProps, slotProps, ...props }) => {
  const resolvedContentProps = mergeContentProps(contentProps);
  const mergedSlotProps = mergeSlotProps(slotProps);

  return (
    <Modal open={open} onClose={handleClose} slotProps={mergedSlotProps} {...props}>
      <Box {...resolvedContentProps} sx={combineSx(resolvedContentProps.sx, sx)}>
        {children}
      </Box>
    </Modal>
  );
};

export default BaseModal;
