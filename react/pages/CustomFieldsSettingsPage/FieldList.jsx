import React from 'react';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import { Chip, CircularProgress, Switch, Table, TableBody, TableCell, TableHead, TableRow } from '@mui/material';
import { Box, Button, IconButton, Tooltip, Typography } from '../../components';

const formatDefaultValue = (field) => {
  if (field.default_value === null || field.default_value === undefined) {
    return '—';
  }
  if (Array.isArray(field.default_value)) {
    return field.default_value.join(', ');
  }
  if (typeof field.default_value === 'object') {
    return JSON.stringify(field.default_value);
  }
  if (typeof field.default_value === 'boolean') {
    return field.default_value ? 'Yes' : 'No';
  }
  return String(field.default_value);
};

const FieldList = ({
  fields,
  loading,
  onEdit,
  onDelete,
  onToggleEnabled,
  onReorder,
}) => {
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress size={32} />
      </Box>
    );
  }

  if (!fields.length) {
    return (
      <Typography variant="body1" color="text.secondary">
        No fields configured yet.
      </Typography>
    );
  }

  return (
    <Table size="small" sx={{ mt: 2 }}>
      <TableHead>
        <TableRow>
          <TableCell>Label</TableCell>
          <TableCell>Entity</TableCell>
          <TableCell>Type</TableCell>
          <TableCell>Required</TableCell>
          <TableCell>Status</TableCell>
          <TableCell>Default</TableCell>
          <TableCell>Display Order</TableCell>
          <TableCell align="right">Actions</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {fields.map((field, index) => (
          <TableRow key={`${field.isCore ? 'core' : 'custom'}-${field.id || index}`}>
            <TableCell>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Typography variant="body2" sx={{ fontWeight: field.isCore ? 600 : 500 }}>
                  {field.label}
                </Typography>
                {field.isCore ? (
                  <Chip size="small" label="Core" color="default" variant="outlined" />
                ) : null}
              </Box>
            </TableCell>
            <TableCell>{field.entityLabel}</TableCell>
            <TableCell>{field.typeLabel}</TableCell>
            <TableCell>{field.required ? 'Yes' : 'No'}</TableCell>
            <TableCell>
              {field.isCore ? (
                'Enabled'
              ) : field.disabled ? (
                'Disabled (locked)'
              ) : (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Switch
                    size="small"
                    checked={field.enabled}
                    onChange={() => onToggleEnabled(field)}
                    disabled={field.disabled}
                  />
                  <Typography variant="body2">
                    {field.enabled ? 'Enabled' : 'Disabled'}
                  </Typography>
                </Box>
              )}
            </TableCell>
            <TableCell>{formatDefaultValue(field)}</TableCell>
            <TableCell>{field.displayValue}</TableCell>
            <TableCell align="right">
              {!field.isCore && (
                <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                  <Tooltip title="Move up">
                    <span>
                <IconButton
                  aria-label="Move up"
                  size="small"
                  onClick={() => onReorder(field, 'up')}
                  disabled={index === 0}
                >
                        <ArrowUpwardIcon fontSize="inherit" />
                      </IconButton>
                    </span>
                  </Tooltip>
                  <Tooltip title="Move down">
                    <span>
                <IconButton
                  aria-label="Move down"
                  size="small"
                  onClick={() => onReorder(field, 'down')}
                  disabled={index === fields.length - 1}
                >
                        <ArrowDownwardIcon fontSize="inherit" />
                      </IconButton>
                    </span>
                  </Tooltip>
                  <Button size="small" onClick={() => onEdit(field)}>
                    Edit
                  </Button>
                  <Button
                    size="small"
                    color="error"
                    variant="outlined"
                    onClick={() => onDelete(field)}
                  >
                    Delete
                  </Button>
                </Box>
              )}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
};

export default FieldList;
