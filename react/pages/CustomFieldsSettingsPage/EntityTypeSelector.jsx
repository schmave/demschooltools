import React from 'react';
import { Button, Stack } from '../../components';
import { ENTITY_TYPES } from './constants';

const EntityTypeSelector = ({ value, onChange }) => {
  return (
    <Stack direction="row" spacing={1}>
      {ENTITY_TYPES.map((type) => (
        <Button
          key={type.id}
          variant={value === type.id ? 'contained' : 'outlined'}
          onClick={() => onChange(type.id)}
        >
          {type.label}
        </Button>
      ))}
    </Stack>
  );
};

export default EntityTypeSelector;
