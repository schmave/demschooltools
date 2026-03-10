import React from 'react';
import { Stack, Typography } from '../index';

/**
 * Renders a read-only display of a single field value.
 * Entity-agnostic — used for Person, Tag, Family, Company, etc.
 */
const EntityFieldDisplay = ({ fieldDef, value, peopleOptions = [] }) => {
  const { label, fieldType, typeProps = {} } = fieldDef;

  const formattedValue = formatValue(fieldType, value, typeProps, peopleOptions);

  if (formattedValue === null || formattedValue === undefined || formattedValue === '') {
    return null;
  }

  return (
    <Stack spacing={0.25}>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body1">{formattedValue}</Typography>
    </Stack>
  );
};

const formatValue = (fieldType, value, typeProps, peopleOptions) => {
  if (value === null || value === undefined || value === '') {
    return '—';
  }

  switch (fieldType) {
    case 'toggle':
      return value ? 'Yes' : 'No';

    case 'currency':
      return typeof value === 'number' ? `$${value.toFixed(2)}` : String(value);

    case 'date':
      if (typeof value === 'string' && value.length >= 10) {
        return new Date(value + 'T00:00:00').toLocaleDateString();
      }
      return String(value);

    case 'datetime':
      if (typeof value === 'string') {
        return new Date(value).toLocaleString();
      }
      return String(value);

    case 'select':
    case 'radioGroup': {
      const options = typeProps.options || [];
      if (Array.isArray(value)) {
        return value
          .map((v) => options.find((o) => o.id === v)?.label || v)
          .join(', ');
      }
      return options.find((o) => o.id === value)?.label || String(value);
    }

    case 'checkboxGroup': {
      const options = typeProps.options || [];
      if (Array.isArray(value)) {
        return value
          .map((v) => options.find((o) => o.id === v)?.label || v)
          .join(', ');
      }
      return String(value);
    }

    case 'peopleSelect': {
      if (Array.isArray(value)) {
        return value
          .map((v) => peopleOptions.find((o) => String(o.value) === String(v))?.label || v)
          .join(', ');
      }
      return (
        peopleOptions.find((o) => String(o.value) === String(value))?.label ||
        String(value)
      );
    }

    default:
      if (Array.isArray(value)) {
        return value.join(', ');
      }
      return String(value);
  }
};

export default EntityFieldDisplay;
