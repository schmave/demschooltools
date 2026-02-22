import React from 'react';
import { ToggleButton, ToggleButtonGroup } from '@mui/material';
import { ENTITY_TYPES } from './constants';

const EntityTypeSelector = ({ value, onChange }) => {
  const handleChange = (event, newValue) => {
    if (newValue !== null) {
      onChange(newValue);
    }
  };

  return (
    <ToggleButtonGroup
      value={value}
      exclusive
      onChange={handleChange}
      size="small"
      sx={{ alignSelf: 'flex-start' }}
    >
      {ENTITY_TYPES.map((type) => (
        <ToggleButton key={type.id} value={type.id} sx={{ px: 3, textTransform: 'none' }}>
          {type.label}
        </ToggleButton>
      ))}
    </ToggleButtonGroup>
  );
};

export default EntityTypeSelector;
