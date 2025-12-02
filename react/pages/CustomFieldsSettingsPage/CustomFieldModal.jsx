import React from 'react';
import { Alert, Divider } from '../../components';
import {
  Button,
  Checkbox,
  FormControlLabel,
  SelectInput,
  Stack,
  TextField,
  Typography,
} from '../../components';
import { InfoModal } from '../../containers';
import OptionsEditor from './OptionsEditor';
import DefaultValueInput from './DefaultValueInput';
import { CONDITION_PLACEHOLDER, FIELD_TYPE_OPTIONS } from './constants';

const defaultFormState = (entityType) => ({
  id: undefined,
  entity_type: entityType,
  field_type: 'text',
  label: '',
  help_text: '',
  required: false,
  enabled: true,
  display_order: '',
  type_props: {},
  type_validation: {},
  default_value: null,
  required_if: [],
  disabled_if: [],
  visible_to_role_ids: [],
  editable_by_role_ids: [],
});

const formatJson = (value) => {
  if (!value || !Array.isArray(value) || value.length === 0) {
    return '';
  }
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return '';
  }
};

const getDefaultTypeProps = (fieldType) => {
  switch (fieldType) {
    case 'select':
    case 'radioGroup':
    case 'checkboxGroup':
      return {
        options: [],
        multiSelect: fieldType === 'select' ? false : fieldType === 'checkboxGroup',
      };
    case 'controlledNumber':
      return {
        step: 1,
      };
    case 'peopleSelect':
      return {
        multiSelect: false,
        filterByTags: [],
      };
    default:
      return {};
  }
};

const CustomFieldModal = ({
  open,
  mode,
  entityType,
  field,
  roleOptions,
  tagOptions,
  saving,
  serverError,
  onClose,
  onSubmit,
}) => {
  const [formState, setFormState] = React.useState(() => defaultFormState(entityType));
  const [jsonText, setJsonText] = React.useState({
    required_if: '',
    disabled_if: '',
  });
  const [errors, setErrors] = React.useState({});
  const [jsonErrors, setJsonErrors] = React.useState({});
  const [defaultValueError, setDefaultValueError] = React.useState('');

  React.useEffect(() => {
    const nextState = field
      ? {
          ...field,
          display_order: field.display_order ?? '',
          type_props: { ...(field.type_props || {}) },
          type_validation: { ...(field.type_validation || {}) },
          default_value: field?.default_value ?? null,
        }
      : defaultFormState(entityType);
    setFormState(nextState);
    setJsonText({
      required_if: formatJson(nextState.required_if),
      disabled_if: formatJson(nextState.disabled_if),
    });
    setErrors({});
    setJsonErrors({});
    setDefaultValueError('');
  }, [field, entityType, open]);

  const handleBasicChange = (key) => (event) => {
    const value =
      event.target.type === 'checkbox' ? event.target.checked : event.target.value;
    setFormState((prev) => ({
      ...prev,
      [key]: value,
    }));
  };

  const handleFieldTypeChange = (value) => {
    setFormState((prev) => ({
      ...prev,
      field_type: value,
      type_props: getDefaultTypeProps(value),
      default_value: null,
    }));
    setDefaultValueError('');
  };

  const handleTypePropChange = (key, value) => {
    setFormState((prev) => ({
      ...prev,
      type_props: {
        ...prev.type_props,
        [key]: value,
      },
    }));
  };

  const handleValidationChange = (key, value) => {
    setFormState((prev) => ({
      ...prev,
      type_validation: {
        ...prev.type_validation,
        [key]: value,
      },
    }));
  };

  const normalizeNumber = (value) => {
    if (value === '' || value === null || value === undefined) {
      return null;
    }
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
  };

  const buildPayload = () => {
    const payload = {
      ...formState,
      entity_type: entityType,
      display_order:
        formState.display_order === '' ? null : Number(formState.display_order),
      type_validation: {},
    };

    Object.entries(formState.type_validation || {}).forEach(([key, value]) => {
      if (value === '' || value === null || value === undefined) {
        return;
      }
      if (
        ['min', 'max', 'minLength', 'maxLength', 'minSelected', 'maxSelected'].includes(
          key,
        )
      ) {
        payload.type_validation[key] = normalizeNumber(value);
      } else {
        payload.type_validation[key] = value;
      }
    });

    if (Object.keys(payload.type_validation).length === 0) {
      payload.type_validation = {};
    }

    return payload;
  };

  const validate = () => {
    const newErrors = {};
    const newJsonErrors = {};

    if (!formState.label.trim()) {
      newErrors.label = 'Label is required.';
    }
    if (!formState.field_type) {
      newErrors.field_type = 'Field type is required.';
    }

    const requiresOptions = ['select', 'radioGroup', 'checkboxGroup'].includes(
      formState.field_type,
    );
    if (requiresOptions) {
      const options = formState.type_props?.options || [];
      if (options.length === 0) {
        newErrors.type_props = 'At least one option is required.';
      } else if (!options.some((option) => option.enabled !== false)) {
        newErrors.type_props = 'At least one option must be enabled.';
      } else if (options.some((option) => !option.id || !option.label)) {
        newErrors.type_props = 'Each option needs an ID and label.';
      }
    }

    if (formState.field_type === 'select' && formState.type_props?.multiSelect === false) {
      const maxSelected = normalizeNumber(formState.type_validation?.maxSelected);
      if (maxSelected && maxSelected > 1) {
        newErrors.type_validation = 'Max selections must be 1 when multi-select is off.';
      }
    }

    if (['integer', 'number', 'controlledNumber', 'currency'].includes(formState.field_type)) {
      const min = normalizeNumber(formState.type_validation?.min);
      const max = normalizeNumber(formState.type_validation?.max);
      if (min !== null && max !== null && min > max) {
        newErrors.type_validation = 'Min value must be less than or equal to max value.';
      }
    }

    if (formState.field_type === 'text') {
      const minLength = normalizeNumber(formState.type_validation?.minLength);
      const maxLength = normalizeNumber(formState.type_validation?.maxLength);
      if (minLength !== null && maxLength !== null && minLength > maxLength) {
        newErrors.type_validation = 'Min length must be less than or equal to max length.';
      }
    }

    const parsedRequired = parseConditions(jsonText.required_if, 'required_if', newJsonErrors);
    const parsedDisabled = parseConditions(jsonText.disabled_if, 'disabled_if', newJsonErrors);

    setErrors(newErrors);
    setJsonErrors(newJsonErrors);

    return {
      isValid: Object.keys(newErrors).length === 0 && Object.keys(newJsonErrors).length === 0,
      parsedRequired,
      parsedDisabled,
    };
  };

  const parseConditions = (value, key, errorBag) => {
    if (!value || !value.trim()) {
      return [];
    }
    try {
      const parsed = JSON.parse(value);
      if (!Array.isArray(parsed)) {
        throw new Error('Value must be an array.');
      }
      return parsed;
    } catch (error) {
      errorBag[key] = 'Must be valid JSON array.';
      return [];
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const { isValid, parsedRequired, parsedDisabled } = validate();
    if (!isValid) {
      return;
    }
    const payload = buildPayload();
    payload.required_if = parsedRequired;
    payload.disabled_if = parsedDisabled;
    onSubmit(payload);
  };

  const handleDefaultValueChange = (newValue) => {
    setDefaultValueError('');
    setFormState((prev) => ({
      ...prev,
      default_value: normalizeDefaultForType(prev.field_type, newValue, prev.type_props, setDefaultValueError),
    }));
  };

  const renderTypeSettings = () => {
    switch (formState.field_type) {
      case 'select':
        return (
          <Stack spacing={2}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={Boolean(formState.type_props?.multiSelect)}
                  onChange={(event) => handleTypePropChange('multiSelect', event.target.checked)}
                />
              }
              label="Allow multiple selections"
            />
            <OptionsEditor
              options={formState.type_props?.options || []}
              onChange={(options) => handleTypePropChange('options', options)}
            />
          </Stack>
        );
      case 'radioGroup':
      case 'checkboxGroup':
        return (
          <OptionsEditor
            options={formState.type_props?.options || []}
            onChange={(options) => handleTypePropChange('options', options)}
          />
        );
      case 'controlledNumber':
        return (
          <TextField
            label="Step"
            type="number"
            value={formState.type_props?.step ?? 1}
            onChange={(event) => handleTypePropChange('step', event.target.value)}
            size="small"
          />
        );
      case 'peopleSelect':
        return (
          <Stack spacing={2}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={Boolean(formState.type_props?.multiSelect)}
                  onChange={(event) => handleTypePropChange('multiSelect', event.target.checked)}
                />
              }
              label="Allow selecting multiple people"
            />
            <SelectInput
              label="Filter by tags"
              multiple
              autocomplete
              value={formState.type_props?.filterByTags || []}
              options={tagOptions}
              onChange={(event, values) => handleTypePropChange('filterByTags', values)}
              placeholder="Limit people shown by tags"
            />
          </Stack>
        );
      default:
        return null;
    }
  };

  const renderValidation = () => {
    const fields = [];
    if (['integer', 'number', 'controlledNumber', 'currency'].includes(formState.field_type)) {
      fields.push(
        <TextField
          key="min"
          label="Min value"
          type="number"
          value={formState.type_validation?.min ?? ''}
          onChange={(event) => handleValidationChange('min', event.target.value)}
          size="small"
        />,
        <TextField
          key="max"
          label="Max value"
          type="number"
          value={formState.type_validation?.max ?? ''}
          onChange={(event) => handleValidationChange('max', event.target.value)}
          size="small"
        />,
      );
    }
    if (formState.field_type === 'currency') {
      fields.push(
        <FormControlLabel
          key="allowNegative"
          control={
            <Checkbox
              checked={Boolean(formState.type_props?.allowNegative)}
              onChange={(event) => handleTypePropChange('allowNegative', event.target.checked)}
            />
          }
          label="Allow negative values"
        />,
      );
    }
    if (formState.field_type === 'text') {
      fields.push(
        <TextField
          key="minLength"
          label="Min length"
          type="number"
          value={formState.type_validation?.minLength ?? ''}
          onChange={(event) => handleValidationChange('minLength', event.target.value)}
          size="small"
        />,
        <TextField
          key="maxLength"
          label="Max length"
          type="number"
          value={formState.type_validation?.maxLength ?? ''}
          onChange={(event) => handleValidationChange('maxLength', event.target.value)}
          size="small"
        />,
        <TextField
          key="requiredPattern"
          label="Regular expression"
          value={formState.type_validation?.requiredPattern ?? ''}
          onChange={(event) => handleValidationChange('requiredPattern', event.target.value)}
          size="small"
        />,
        <TextField
          key="errorMessage"
          label="Pattern error message"
          value={formState.type_validation?.errorMessage ?? ''}
          onChange={(event) => handleValidationChange('errorMessage', event.target.value)}
          size="small"
        />,
      );
    }
    if (
      ['select', 'checkboxGroup', 'peopleSelect'].includes(formState.field_type)
    ) {
      fields.push(
        <TextField
          key="minSelected"
          label="Min selections"
          type="number"
          value={formState.type_validation?.minSelected ?? ''}
          onChange={(event) => handleValidationChange('minSelected', event.target.value)}
          size="small"
        />,
        <TextField
          key="maxSelected"
          label="Max selections"
          type="number"
          value={formState.type_validation?.maxSelected ?? ''}
          onChange={(event) => handleValidationChange('maxSelected', event.target.value)}
          size="small"
        />,
      );
    }
    if (['date', 'datetime'].includes(formState.field_type)) {
      fields.push(
        <TextField
          key="minDate"
          label="Earliest allowed date (YYYY-MM-DD)"
          value={formState.type_validation?.minDate ?? ''}
          onChange={(event) => handleValidationChange('minDate', event.target.value)}
          size="small"
        />,
        <TextField
          key="maxDate"
          label="Latest allowed date (YYYY-MM-DD)"
          value={formState.type_validation?.maxDate ?? ''}
          onChange={(event) => handleValidationChange('maxDate', event.target.value)}
          size="small"
        />,
      );
    }

    if (fields.length === 0) {
      return null;
    }

    return (
      <Stack spacing={2}>
        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
          Validation
        </Typography>
        <Stack spacing={1}>{fields}</Stack>
        {errors.type_validation ? (
          <Typography color="error" variant="body2">
            {errors.type_validation}
          </Typography>
        ) : null}
      </Stack>
    );
  };

  return (
    <InfoModal
      open={open}
      onClose={onClose}
      title={mode === 'edit' ? 'Edit Custom Field' : 'Create Custom Field'}
      contentProps={{ sx: { width: { xs: '100%', md: 900 } } }}
      actions={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" disabled={saving}>
            {saving ? 'Saving...' : 'Save'}
          </Button>
        </>
      }
    >
      <Stack spacing={3} component="form" onSubmit={handleSubmit}>
        {serverError ? (
          <Alert severity="error">{serverError}</Alert>
        ) : null}

        <Stack spacing={2}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            General
          </Typography>
          <TextField label="Entity Type" value={entityType} disabled size="small" />
          <SelectInput
            label="Field Type"
            value={formState.field_type}
            onChange={(event) => handleFieldTypeChange(event.target.value)}
            options={FIELD_TYPE_OPTIONS.map((option) => ({
              value: option.value,
              label: option.label,
            }))}
          />
          {errors.field_type ? (
            <Typography color="error" variant="body2">
              {errors.field_type}
            </Typography>
          ) : null}
          <TextField
            label="Label"
            value={formState.label}
            onChange={handleBasicChange('label')}
            error={Boolean(errors.label)}
            helperText={errors.label}
            size="small"
          />
          <TextField
            label="Help text"
            value={formState.help_text}
            onChange={handleBasicChange('help_text')}
            multiline
            minRows={2}
          />
          <TextField
            label="Display order"
            type="number"
            value={formState.display_order}
            onChange={handleBasicChange('display_order')}
            helperText="Lower numbers appear earlier in the list."
            size="small"
          />
          <Stack direction="row" spacing={2}>
            <FormControlLabel
              control={<Checkbox checked={formState.required} onChange={handleBasicChange('required')} />}
              label="Required"
            />
            <FormControlLabel
              control={<Checkbox checked={formState.enabled} onChange={handleBasicChange('enabled')} />}
              label="Enabled"
            />
          </Stack>
        </Stack>

        <Divider />

        {renderTypeSettings()}
        {errors.type_props ? (
          <Typography color="error" variant="body2">
            {errors.type_props}
          </Typography>
        ) : null}

        <Divider />

        <Stack spacing={2}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            Default Value
          </Typography>
          <DefaultValueInput
            fieldType={formState.field_type}
            typeProps={formState.type_props}
            options={formState.type_props?.options || []}
            value={formState.default_value}
            onChange={handleDefaultValueChange}
          />
          {defaultValueError ? (
            <Typography color="error" variant="body2">
              {defaultValueError}
            </Typography>
          ) : null}
        </Stack>

        <Divider />

        {renderValidation()}

        <Divider />

        <Stack spacing={2}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            Conditional Rules
          </Typography>
          <TextField
            label="Required if (JSON array)"
            multiline
            minRows={3}
            placeholder={CONDITION_PLACEHOLDER}
            value={jsonText.required_if}
            onChange={(event) =>
              setJsonText((prev) => ({ ...prev, required_if: event.target.value }))
            }
            error={Boolean(jsonErrors.required_if)}
            helperText={jsonErrors.required_if}
          />
          <TextField
            label="Disabled if (JSON array)"
            multiline
            minRows={3}
            placeholder={CONDITION_PLACEHOLDER}
            value={jsonText.disabled_if}
            onChange={(event) =>
              setJsonText((prev) => ({ ...prev, disabled_if: event.target.value }))
            }
            error={Boolean(jsonErrors.disabled_if)}
            helperText={jsonErrors.disabled_if}
          />
        </Stack>

        <Divider />

        <Stack spacing={2}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            Roles
          </Typography>
          <SelectInput
            label="Visible to roles"
            multiple
            autocomplete
            value={formState.visible_to_role_ids || []}
            onChange={(event, values) => {
              setFormState((prev) => ({
                ...prev,
                visible_to_role_ids: values,
              }));
            }}
            options={roleOptions}
            placeholder="Defaults to all roles"
          />
          <SelectInput
            label="Editable by roles"
            multiple
            autocomplete
            value={formState.editable_by_role_ids || []}
            onChange={(event, values) => {
              setFormState((prev) => ({
                ...prev,
                editable_by_role_ids: values,
              }));
            }}
            options={roleOptions}
            placeholder="Defaults to all roles"
          />
        </Stack>
      </Stack>
    </InfoModal>
  );
};

export default CustomFieldModal;

const normalizeDefaultForType = (fieldType, value, typeProps, setError) => {
  const multiSelect = Boolean(typeProps?.multiSelect);
  const options = typeProps?.options || [];
  const optionIds = new Set(options.map((option) => option.id));

  if (fieldType === 'toggle') {
    return Boolean(value);
  }

  if (fieldType === 'integer') {
    if (value === '' || value === null) {
      return null;
    }
    const parsed = Number(value);
    if (Number.isNaN(parsed)) {
      setError?.('Default must be a valid integer.');
      return null;
    }
    return Math.trunc(parsed);
  }

  if (
    fieldType === 'number' ||
    fieldType === 'controlledNumber' ||
    fieldType === 'currency'
  ) {
    if (value === '' || value === null) {
      return null;
    }
    const parsed = Number(value);
    if (Number.isNaN(parsed)) {
      setError?.('Default must be numeric.');
      return null;
    }
    return parsed;
  }

  if (fieldType === 'select' || fieldType === 'radioGroup') {
    if (multiSelect) {
      const next = Array.isArray(value) ? value : [];
      const invalid = next.filter((item) => !optionIds.has(item));
      if (invalid.length) {
        setError?.('Default must reference existing options.');
        return [];
      }
      return next;
    }
    if (value === '') {
      return null;
    }
    if (value && optionIds.size > 0 && !optionIds.has(value)) {
      setError?.('Default must reference an existing option.');
      return null;
    }
    return value || null;
  }

  if (fieldType === 'checkboxGroup') {
    const next = Array.isArray(value) ? value : [];
    const invalid = next.filter((item) => !optionIds.has(item));
    if (invalid.length) {
      setError?.('Default must reference existing options.');
      return [];
    }
    return next;
  }

  if (fieldType === 'peopleSelect') {
    if (multiSelect) {
      return Array.isArray(value) ? value : [];
    }
    return value || null;
  }

  if (value === '') {
    return null;
  }
  return value;
};
