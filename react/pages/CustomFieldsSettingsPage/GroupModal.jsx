import React from 'react';
import { Alert } from '../../components';
import { Button, Stack, TextField } from '../../components';
import { InfoModal } from '../../containers';

const defaultFormState = (entityType) => ({
  id: undefined,
  entity_type: entityType,
  label: '',
});

const GroupModal = ({
  open,
  mode,
  entityType,
  group,
  saving,
  serverError,
  onClose,
  onSubmit,
}) => {
  const [formState, setFormState] = React.useState(() => defaultFormState(entityType));
  const [errors, setErrors] = React.useState({});

  React.useEffect(() => {
    const nextState = group
      ? { id: group.id, entity_type: group.entity_type, label: group.label }
      : defaultFormState(entityType);
    setFormState(nextState);
    setErrors({});
  }, [group, entityType, open]);

  const handleChange = (key) => (event) => {
    setFormState((prev) => ({ ...prev, [key]: event.target.value }));
  };

  const validate = () => {
    const newErrors = {};
    if (!formState.label.trim()) {
      newErrors.label = 'Label is required.';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    if (!validate()) return;
    onSubmit({
      ...formState,
      entity_type: entityType,
    });
  };

  return (
    <InfoModal
      open={open}
      onClose={onClose}
      title={mode === 'edit' ? 'Edit Group' : 'Create Group'}
      contentProps={{ sx: { width: { xs: '100%', md: 500 } } }}
      actions={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button onClick={handleSubmit} variant="contained" disabled={saving}>
            {saving ? 'Saving...' : 'Save'}
          </Button>
        </>
      }
    >
      <Stack spacing={3} sx={{ mt: 1 }} component="form" onSubmit={handleSubmit}>
        {serverError ? <Alert severity="error">{serverError}</Alert> : null}

        <TextField
          label="Label"
          value={formState.label}
          onChange={handleChange('label')}
          error={Boolean(errors.label)}
          helperText={errors.label}
          size="small"
        />
      </Stack>
    </InfoModal>
  );
};

export default GroupModal;
