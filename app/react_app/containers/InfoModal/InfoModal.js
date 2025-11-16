import React from 'react';
import CloseIcon from '@mui/icons-material/Close';
import { Box, IconButton, Stack, Typography } from '../../components';
import BaseModal from '../../components/BaseModal';
import { combineSx } from '../../utils';

const InfoModal = ({
  open,
  title,
  onClose,
  children,
  actions,
  contentProps,
  ...props
}) => {
  const titleId = React.useId();
  const { sx: contentSx, ...restContentProps } = contentProps || {};
  const mergedContentProps = {
    ...restContentProps,
    sx: combineSx({ p: { xs: 3, md: 4 } }, contentSx),
  };

  return (
    <BaseModal
      open={open}
      handleClose={onClose}
      aria-labelledby={title ? titleId : undefined}
      contentProps={mergedContentProps}
      {...props}
    >
      <Stack variant="modalHeader">
        {title ? (
          <Typography id={titleId} variant="h4" component="h2" sx={{ fontWeight: 600 }}>
            {title}
          </Typography>
        ) : (
          <Box />
        )}
        <IconButton onClick={onClose} aria-label="Close dialog" size="small">
          <CloseIcon />
        </IconButton>
      </Stack>

      <Box variant="modalBody">{children}</Box>

      {actions ? <Stack variant="modalFooter">{actions}</Stack> : null}
    </BaseModal>
  );
};

export default InfoModal;
