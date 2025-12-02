import React from 'react';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import DeleteIcon from '@mui/icons-material/Delete';
import { Button, Checkbox, IconButton, Stack, TextField, Tooltip, Typography } from '../../components';

const defaultOption = () => ({
  id: '',
  label: '',
  enabled: true,
});

const OptionsEditor = ({ options = [], onChange }) => {
  const handleUpdate = (index, key, value) => {
    const next = options.map((option, optionIndex) =>
      optionIndex === index ? { ...option, [key]: value } : option,
    );
    onChange(next);
  };

  const handleToggle = (index) => {
    handleUpdate(index, 'enabled', !options[index].enabled);
  };

  const handleAdd = () => {
    onChange([...options, defaultOption()]);
  };

  const handleDelete = (index) => {
    onChange(options.filter((_, optionIndex) => optionIndex !== index));
  };

  const handleMove = (index, direction) => {
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= options.length) {
      return;
    }
    const next = [...options];
    const [item] = next.splice(index, 1);
    next.splice(targetIndex, 0, item);
    onChange(next);
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" alignItems="center" justifyContent="space-between">
        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
          Options
        </Typography>
        <Button onClick={handleAdd} variant="outlined">
          Add Option
        </Button>
      </Stack>

      {options.length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          No options yet. Click &ldquo;Add Option&rdquo; to create one.
        </Typography>
      ) : (
        options.map((option, index) => (
          <Stack
            key={`${option.id || 'option'}-${index}`}
            direction="row"
            spacing={1}
            alignItems="center"
          >
            <TextField
              label="Option ID"
              value={option.id}
              onChange={(event) => handleUpdate(index, 'id', event.target.value)}
              size="small"
            />
            <TextField
              label="Label"
              value={option.label}
              onChange={(event) => handleUpdate(index, 'label', event.target.value)}
              size="small"
              sx={{ flex: 1 }}
            />
            <Tooltip title={option.enabled ? 'Disable option' : 'Enable option'}>
              <Checkbox checked={option.enabled} onChange={() => handleToggle(index)} />
            </Tooltip>
            <Tooltip title="Move up">
              <span>
                <IconButton
                  onClick={() => handleMove(index, 'up')}
                  disabled={index === 0}
                  size="small"
                >
                  <ArrowUpwardIcon fontSize="inherit" />
                </IconButton>
              </span>
            </Tooltip>
            <Tooltip title="Move down">
              <span>
                <IconButton
                  onClick={() => handleMove(index, 'down')}
                  disabled={index === options.length - 1}
                  size="small"
                >
                  <ArrowDownwardIcon fontSize="inherit" />
                </IconButton>
              </span>
            </Tooltip>
            <Tooltip title="Remove option">
              <IconButton onClick={() => handleDelete(index)} color="error" size="small">
                <DeleteIcon fontSize="inherit" />
              </IconButton>
            </Tooltip>
          </Stack>
        ))
      )}
    </Stack>
  );
};

export default OptionsEditor;
