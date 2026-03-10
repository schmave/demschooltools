import React from 'react';
import DeleteIcon from '@mui/icons-material/Delete';
import { Button, IconButton, Stack, TextField, Tooltip, Typography } from '../../components';
import { formatPhoneNumber } from '../../utils';

const MAX_PHONES = 3;

const emptyPhone = () => ({ number: '', comment: '' });

const PhoneNumbersInput = ({ phones = [], onChange, readOnly = false }) => {
  const entries = phones.length > 0 ? phones : [emptyPhone()];

  const handleChange = (index, key, value) => {
    const next = entries.map((p, i) => (i === index ? { ...p, [key]: value } : p));
    onChange(next);
  };

  const handleAdd = () => {
    if (entries.length >= MAX_PHONES) return;
    onChange([...entries, emptyPhone()]);
  };

  const handleRemove = (index) => {
    const next = entries.filter((_, i) => i !== index);
    onChange(next.length === 0 ? [emptyPhone()] : next);
  };

  if (readOnly) {
    const filled = entries.filter((p) => p.number || p.comment);
    if (filled.length === 0) return null;
    return (
      <Stack spacing={1}>
        <Typography variant="caption" color="text.secondary">
          Phone Numbers
        </Typography>
        {filled.map((phone, index) => (
          <Typography key={index} variant="body1">
            {formatPhoneNumber(phone.number)}{phone.comment ? ` (${phone.comment})` : ''}
          </Typography>
        ))}
      </Stack>
    );
  }

  return (
    <Stack spacing={1.5}>
      <Stack direction="row" alignItems="center" justifyContent="space-between">
        <Typography variant="body2" color="text.secondary">
          Phone Numbers
        </Typography>
        {entries.length < MAX_PHONES && (
          <Button size="small" onClick={handleAdd}>
            Add Phone
          </Button>
        )}
      </Stack>
      {entries.map((phone, index) => (
        <Stack key={index} direction="row" spacing={1} alignItems="center">
          <TextField
            label={`Phone ${index + 1}`}
            value={phone.number}
            onChange={(e) => handleChange(index, 'number', e.target.value)}
            size="small"
            sx={{ flex: 1 }}
          />
          <TextField
            label="Comment"
            value={phone.comment}
            onChange={(e) => handleChange(index, 'comment', e.target.value)}
            size="small"
            sx={{ flex: 1 }}
          />
          {entries.length > 1 && (
            <Tooltip title="Remove phone">
              <IconButton
                onClick={() => handleRemove(index)}
                color="error"
                size="small"
              >
                <DeleteIcon fontSize="inherit" />
              </IconButton>
            </Tooltip>
          )}
        </Stack>
      ))}
    </Stack>
  );
};

export default PhoneNumbersInput;
