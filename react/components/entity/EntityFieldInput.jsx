import React from 'react';
import {
  Checkbox,
  FormControlLabel,
  Radio,
  RadioGroup,
  SelectInput,
  Stack,
  TextField,
  Typography,
} from '../index';

/**
 * Renders an editable input for a single field, based on fieldDef.fieldType.
 * Entity-agnostic — used for Person, Tag, Family, Company, etc.
 */
const EntityFieldInput = ({ fieldDef, value, onChange, error, disabled, peopleOptions = [] }) => {
  const { fieldType, label, helpText, required, typeProps = {} } = fieldDef;
  const requiredLabel = required ? `${label} *` : label;

  switch (fieldType) {
    case 'text':
      return (
        <TextField
          fullWidth
          label={requiredLabel}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
          error={Boolean(error)}
          helperText={error || helpText}
          disabled={disabled}
          multiline={Boolean(typeProps.multiline)}
          minRows={typeProps.multiline ? 3 : undefined}
          size="small"
        />
      );

    case 'integer':
      return (
        <TextField
          fullWidth
          label={requiredLabel}
          type="number"
          inputProps={{ step: 1 }}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
          error={Boolean(error)}
          helperText={error || helpText}
          disabled={disabled}
          size="small"
        />
      );

    case 'number':
      return (
        <TextField
          fullWidth
          label={requiredLabel}
          type="number"
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
          error={Boolean(error)}
          helperText={error || helpText}
          disabled={disabled}
          size="small"
        />
      );

    case 'controlledNumber':
      return (
        <TextField
          fullWidth
          label={requiredLabel}
          type="number"
          inputProps={{ step: typeProps.step ?? 1 }}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
          error={Boolean(error)}
          helperText={error || helpText}
          disabled={disabled}
          size="small"
        />
      );

    case 'currency':
      return (
        <TextField
          fullWidth
          label={requiredLabel}
          type="number"
          inputProps={{ step: '0.01' }}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
          error={Boolean(error)}
          helperText={error || helpText}
          disabled={disabled}
          size="small"
        />
      );

    case 'date':
      return (
        <TextField
          fullWidth
          label={requiredLabel}
          type="date"
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value || null)}
          error={Boolean(error)}
          helperText={error || helpText}
          disabled={disabled}
          size="small"
          InputLabelProps={{ shrink: true }}
        />
      );

    case 'datetime':
      return (
        <TextField
          fullWidth
          label={requiredLabel}
          type="datetime-local"
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value || null)}
          error={Boolean(error)}
          helperText={error || helpText}
          disabled={disabled}
          size="small"
          InputLabelProps={{ shrink: true }}
        />
      );

    case 'toggle':
      return (
        <Stack>
          <FormControlLabel
            control={
              <Checkbox
                checked={Boolean(value)}
                onChange={(e) => onChange(e.target.checked)}
                disabled={disabled}
              />
            }
            label={requiredLabel}
          />
          {(error || helpText) ? (
            <Typography variant="caption" color={error ? 'error' : 'text.secondary'}>
              {error || helpText}
            </Typography>
          ) : null}
        </Stack>
      );

    case 'radioGroup': {
      const options = typeProps.options || [];
      return (
        <Stack spacing={0.5}>
          <Typography variant="body2" color={error ? 'error' : 'text.secondary'}>
            {requiredLabel}
          </Typography>
          <RadioGroup
            value={value ?? ''}
            onChange={(e) => onChange(e.target.value)}
          >
            {options.filter((o) => o.enabled !== false).map((option) => (
              <FormControlLabel
                key={option.id}
                value={option.id}
                control={<Radio size="small" disabled={disabled} />}
                label={option.label}
              />
            ))}
          </RadioGroup>
          {(error || helpText) ? (
            <Typography variant="caption" color={error ? 'error' : 'text.secondary'}>
              {error || helpText}
            </Typography>
          ) : null}
        </Stack>
      );
    }

    case 'checkboxGroup': {
      const options = typeProps.options || [];
      const selected = Array.isArray(value) ? value : [];
      const handleToggle = (optionId) => {
        const next = selected.includes(optionId)
          ? selected.filter((id) => id !== optionId)
          : [...selected, optionId];
        onChange(next);
      };
      return (
        <Stack spacing={0.5}>
          <Typography variant="body2" color={error ? 'error' : 'text.secondary'}>
            {requiredLabel}
          </Typography>
          {options.filter((o) => o.enabled !== false).map((option) => (
            <FormControlLabel
              key={option.id}
              control={
                <Checkbox
                  size="small"
                  checked={selected.includes(option.id)}
                  onChange={() => handleToggle(option.id)}
                  disabled={disabled}
                />
              }
              label={option.label}
            />
          ))}
          {(error || helpText) ? (
            <Typography variant="caption" color={error ? 'error' : 'text.secondary'}>
              {error || helpText}
            </Typography>
          ) : null}
        </Stack>
      );
    }

    case 'select': {
      const options = (typeProps.options || [])
        .filter((o) => o.enabled !== false)
        .map((o) => ({ value: o.id, label: o.label }));
      const multi = Boolean(typeProps.multiSelect);
      return (
        <SelectInput
          label={requiredLabel}
          value={multi ? (Array.isArray(value) ? value : []) : (value ?? '')}
          onChange={(_event, newVal) => onChange(newVal)}
          options={options}
          multiple={multi}
          autocomplete
          disabled={disabled}
          placeholder={helpText}
          size="small"
          marginTop="0px"
          marginBottom="0px"
        />
      );
    }

    case 'peopleSelect': {
      const multi = Boolean(typeProps.multiSelect);
      return (
        <SelectInput
          label={requiredLabel}
          value={multi ? (Array.isArray(value) ? value : []) : (value ?? '')}
          onChange={(_event, newVal) => onChange(newVal)}
          options={peopleOptions}
          multiple={multi}
          autocomplete
          disabled={disabled}
          placeholder={helpText || 'Search people...'}
          size="small"
          marginTop="0px"
          marginBottom="0px"
        />
      );
    }

    default:
      return (
        <TextField
          fullWidth
          label={requiredLabel}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
          error={Boolean(error)}
          helperText={error || helpText}
          disabled={disabled}
          size="small"
        />
      );
  }
};

export default EntityFieldInput;
