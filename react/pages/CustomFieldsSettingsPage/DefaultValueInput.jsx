import React from 'react';
import { Checkbox, FormControlLabel, SelectInput, TextField } from '../../components';

const DefaultValueInput = ({
  fieldType,
  typeProps,
  options = [],
  value,
  onChange,
  multiValuePlaceholder,
}) => {
  const normalizedOptions = React.useMemo(
    () =>
      options.map((option) => ({
        value: option.id,
        label: option.label,
      })),
    [options],
  );

  const isMultiSelect = Boolean(typeProps?.multiSelect);

  if (fieldType === 'toggle') {
    return (
      <FormControlLabel
        control={
          <Checkbox
            checked={Boolean(value)}
            onChange={(event) => onChange(event.target.checked)}
          />
        }
        label="Default to enabled"
      />
    );
  }

  if (
    (fieldType === 'select' || fieldType === 'radioGroup') &&
    normalizedOptions.length > 0
  ) {
    if (isMultiSelect) {
      return (
        <SelectInput
          label="Default options"
          multiple
          value={Array.isArray(value) ? value : []}
          options={normalizedOptions}
          onChange={(event) => onChange(event.target.value)}
        />
      );
    }
    return (
      <SelectInput
        label="Default option"
        value={value || ''}
        options={normalizedOptions}
        onChange={(event) => onChange(event.target.value)}
      />
    );
  }

  if (fieldType === 'checkboxGroup' && normalizedOptions.length > 0) {
    return (
      <SelectInput
        label="Default options"
        multiple
        value={Array.isArray(value) ? value : []}
        options={normalizedOptions}
        onChange={(event) => onChange(event.target.value)}
      />
    );
  }

  if (fieldType === 'peopleSelect') {
    const multi = Boolean(typeProps?.multiSelect);
    if (multi) {
      return (
        <TextField
          label="Default person IDs"
          value={Array.isArray(value) ? value.join(',') : ''}
          onChange={(event) =>
            onChange(
              event.target.value
                ? event.target.value.split(',').map((entry) => entry.trim()).filter(Boolean)
                : [],
            )
          }
          helperText={multiValuePlaceholder || 'Comma-separated person IDs'}
        />
      );
    }
    return (
      <TextField
        label="Default person ID"
        value={value || ''}
        onChange={(event) => onChange(event.target.value)}
      />
    );
  }

  const typeConfig = getInputConfig(fieldType);

  return (
    <TextField
      label="Default value"
      type={typeConfig.type}
      value={value ?? ''}
      onChange={(event) => onChange(event.target.value)}
      helperText={typeConfig.helperText}
      multiline={typeConfig.multiline}
      minRows={typeConfig.multiline ? 2 : undefined}
    />
  );
};

const getInputConfig = (fieldType) => {
  switch (fieldType) {
    case 'integer':
      return { type: 'number', helperText: 'Enter an integer default.' };
    case 'number':
    case 'controlledNumber':
    case 'currency':
      return { type: 'number', helperText: 'Enter a numeric default.' };
    case 'date':
      return { type: 'date' };
    case 'datetime':
      return { type: 'datetime-local' };
    default:
      return { type: 'text' };
  }
};

export default DefaultValueInput;
