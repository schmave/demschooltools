import React from 'react';
import Grid from '@mui/material/Grid';
import { Divider, Paper, Stack, Typography } from '../index';
import EntityFieldDisplay from './EntityFieldDisplay';
import EntityFieldInput from './EntityFieldInput';

/**
 * Returns the Grid size for a field. Fields can declare `columns` (1-12)
 * to control how much horizontal space they take on medium+ screens.
 * Defaults to 6 (half-width). Full-width for multiline text, special,
 * toggle, radioGroup, and checkboxGroup fields.
 */
const getFieldSize = (field) => {
  if (field.columns != null) return { xs: 12, md: field.columns };
  const fullWidthTypes = ['special', 'toggle', 'radioGroup', 'checkboxGroup'];
  if (fullWidthTypes.includes(field.fieldType)) return { xs: 12, md: 12 };
  if (field.fieldType === 'text' && field.typeProps?.multiline) return { xs: 12, md: 12 };
  return { xs: 12, md: 6 };
};

/**
 * Render a single field (shared between grouped and ungrouped rendering).
 */
const renderField = (field, { values, errors, onChange, renderCustomField, readOnly, peopleOptions, disabled }) => {
  const value = values[field.key];
  const error = errors[field.key];
  const size = getFieldSize(field);

  if (renderCustomField) {
    const custom = renderCustomField(
      field.key,
      field,
      value,
      (val) => onChange(field.key, val),
      error,
    );
    if (custom !== undefined && custom !== null) {
      return (
        <Grid key={field.key} size={size}>
          {custom}
        </Grid>
      );
    }
  }

  if (field.fieldType === 'special') return null;

  if (readOnly) {
    return (
      <Grid key={field.key} size={size}>
        <EntityFieldDisplay
          fieldDef={field}
          value={value}
          peopleOptions={peopleOptions}
        />
      </Grid>
    );
  }

  return (
    <Grid key={field.key} size={size}>
      <EntityFieldInput
        fieldDef={field}
        value={value}
        onChange={(val) => onChange(field.key, val)}
        error={error}
        disabled={disabled || field.disabled}
        peopleOptions={peopleOptions}
      />
    </Grid>
  );
};

/**
 * Renders a form (or read-only display) from field definitions, organized
 * by DB-driven groups. Entity-agnostic.
 *
 * Props:
 *   fieldDefinitions  - sorted array of unified field defs (core + custom)
 *   values            - {key: value} map
 *   errors            - {key: errorMessage} map
 *   onChange(key, val) - setter for a single field
 *   groups            - array of group objects from the API
 *   renderCustomField - escape hatch for special fields
 *   readOnly          - if true, renders EntityFieldDisplay instead
 *   peopleOptions     - [{value, label}] for peopleSelect fields
 *   disabled          - disable all inputs
 */
const EntityForm = ({
  fieldDefinitions,
  values,
  errors = {},
  onChange,
  groups = [],
  renderCustomField,
  readOnly = false,
  peopleOptions = [],
  disabled = false,
}) => {
  const fieldProps = { values, errors, onChange, renderCustomField, readOnly, peopleOptions, disabled };

  // Sort groups by display_order
  const sortedGroups = React.useMemo(
    () => [...groups].sort((a, b) => a.display_order - b.display_order),
    [groups],
  );

  // Build field lists per group
  const groupFieldLists = React.useMemo(() => {
    const fieldsByKey = {};
    for (const f of fieldDefinitions) {
      fieldsByKey[f.key] = f;
    }

    return sortedGroups.map((group) => {
      const hiddenKeys = new Set(group.hidden_core_field_keys || []);
      // core_field_keys is the unified field order — contains both core keys
      // ("first_name") and custom field refs ("cf_5")
      const orderedFields = (group.core_field_keys || [])
        .filter((k) => !hiddenKeys.has(k))
        .map((k) => fieldsByKey[k])
        .filter(Boolean);
      // Append any custom fields in this group not yet in the order array
      const orderedKeySet = new Set(group.core_field_keys || []);
      const unordered = fieldDefinitions
        .filter(
          (f) => !f.isCore && f.groupId === group.id && !orderedKeySet.has(f.key),
        )
        .sort((a, b) => {
          const orderA = a.displayOrder ?? Number.MAX_SAFE_INTEGER;
          const orderB = b.displayOrder ?? Number.MAX_SAFE_INTEGER;
          if (orderA !== orderB) return orderA - orderB;
          return a.label.localeCompare(b.label);
        });
      const allFields = [...orderedFields, ...unordered];
      return { group, fields: allFields };
    });
  }, [sortedGroups, fieldDefinitions]);

  // If no groups defined, render flat (backwards compatible)
  if (sortedGroups.length === 0) {
    return (
      <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 } }}>
        <Grid container spacing={2}>
          {fieldDefinitions.map((field) => renderField(field, fieldProps))}
        </Grid>
      </Paper>
    );
  }

  return (
    <Stack spacing={1.5}>
      {groupFieldLists.map(({ group, fields }) => {
        if (fields.length === 0) return null;
        return (
          <Paper key={group.id} variant="outlined" sx={{ p: { xs: 1, md: 1.5 } }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 0.5 }}>
              {group.label}
            </Typography>
            <Divider sx={{ mb: 1 }} />
            <Grid container spacing={1}>
              {fields.map((field) => renderField(field, fieldProps))}
            </Grid>
          </Paper>
        );
      })}
    </Stack>
  );
};

export default EntityForm;
