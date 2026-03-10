import React from 'react';
import { Alert, Button, PageTitle, PageWrapper, SelectInput, Stack } from '../../components';
import {
  EntityForm,
  buildFieldDefinitions,
  getInitialValues,
  useEntityForm,
} from '../../components/entity';
import { SnackbarContext } from '../../contexts';
import { safeParse } from '../../utils';
import PhoneNumbersInput from './PhoneNumbersInput';
import { createPerson, updatePerson } from './api';
import { CORE_PERSON_FIELDS } from './personFields';

const getInitialData = () => {
  if (typeof window === 'undefined') return {};
  return window.initialData || {};
};

const PersonFormPage = () => {
  const initialData = getInitialData();
  const mode = initialData.mode || 'create';
  const personApiUrl = initialData.personApiUrl || '/api/people/';
  const groups = safeParse(initialData.groups, []);
  const customFields = safeParse(initialData.customFields, []);
  const tagOptions = safeParse(initialData.tagOptions, []).map((t) => ({
    value: t.id,
    label: t.label,
  }));
  const peopleOptions = safeParse(initialData.peopleOptions, []).map((p) => ({
    value: p.id,
    label: p.label,
  }));
  const showElectronicSignin = initialData.showElectronicSignin ?? false;

  const existingPerson = safeParse(initialData.person, null);
  const existingCfValues = safeParse(initialData.customFieldValues, {});

  const { setSnackbar } = React.useContext(SnackbarContext);

  // Filter out PIN field if electronic signin is not enabled
  const coreFields = React.useMemo(
    () =>
      CORE_PERSON_FIELDS.filter(
        (f) => !f.conditionalOn || (f.conditionalOn === 'showElectronicSignin' && showElectronicSignin),
      ),
    [showElectronicSignin],
  );

  const fieldDefinitions = React.useMemo(
    () => buildFieldDefinitions(coreFields, customFields),
    [coreFields, customFields],
  );

  // Build initial values
  const initialValues = React.useMemo(() => {
    const coreValues = existingPerson || {};
    const vals = getInitialValues(fieldDefinitions, coreValues, existingCfValues);
    // Special fields
    vals.phone_numbers = existingPerson?.phone_numbers || [{ number: '', comment: '' }];
    vals.tags = existingPerson?.tags?.map((t) => t.id) || [];
    vals.family_person_id = existingPerson?.family_person_id || null;
    return vals;
  }, [fieldDefinitions, existingPerson, existingCfValues]);

  const handleSubmit = async (values) => {
    // Separate core, custom field values, and special fields
    const corePayload = {};
    const cfPayload = {};

    for (const field of fieldDefinitions) {
      if (field.fieldType === 'special') continue;
      let val = values[field.key];
      // Convert empty strings to null for date/numeric fields only
      const nullOnEmpty = ['date', 'datetime', 'integer', 'number', 'controlledNumber', 'currency'];
      if (val === '' && nullOnEmpty.includes(field.fieldType)) {
        val = null;
      }
      if (field.isCore) {
        corePayload[field.key] = val;
      } else if (field.customFieldId) {
        cfPayload[String(field.customFieldId)] = val;
      }
    }

    const payload = {
      ...corePayload,
      phone_numbers: (values.phone_numbers || []).filter(
        (p) => p.number?.trim() || p.comment?.trim(),
      ),
      tag_ids: values.tags || [],
      family_person_id: values.family_person_id || null,
      custom_field_values: cfPayload,
    };

    if (mode === 'edit' && existingPerson?.id) {
      const result = await updatePerson(personApiUrl, existingPerson.id, payload);
      setSnackbar({ message: 'Person updated successfully.' });
      window.location.href = `/people/${result.id}`;
    } else {
      const result = await createPerson(personApiUrl, payload);
      setSnackbar({ message: 'Person created successfully.' });
      window.location.href = `/people/${result.id}`;
    }
  };

  const form = useEntityForm({
    fieldDefinitions,
    initialValues,
    onSubmit: handleSubmit,
  });

  // Custom renderers for special fields
  const renderCustomField = (key, fieldDef, value, onChange, error) => {
    if (key === 'phone_numbers') {
      return (
        <PhoneNumbersInput
          phones={form.values.phone_numbers}
          onChange={(phones) => form.setValue('phone_numbers', phones)}
        />
      );
    }
    if (key === 'tags') {
      return (
        <Stack spacing={1}>
          <SelectInput
            label="Tags"
            multiple
            autocomplete
            value={form.values.tags || []}
            onChange={(_event, newVal) => form.setValue('tags', newVal)}
            options={tagOptions}
            placeholder="Select tags..."
            marginTop="0px"
            marginBottom="0px"
          />
          {error ? (
            <Alert severity="error">{error}</Alert>
          ) : null}
        </Stack>
      );
    }
    if (key === 'family_person_id') {
      // Filter out current person from sibling options
      const siblingOptions = existingPerson?.id
        ? peopleOptions.filter((p) => p.value !== existingPerson.id)
        : peopleOptions;
      return (
        <SelectInput
          label="Same family as"
          autocomplete
          value={form.values.family_person_id ?? ''}
          onChange={(_event, newVal) => form.setValue('family_person_id', newVal || null)}
          options={siblingOptions}
          placeholder={fieldDef.helpText || 'Search for a sibling...'}
          showClearButton
          marginTop="0px"
          marginBottom="0px"
        />
      );
    }
    return null;
  };

  const isEdit = mode === 'edit';
  const title = isEdit
    ? `Editing ${existingPerson?.display_name || existingPerson?.first_name || ''} ${existingPerson?.last_name || ''}`.trim()
    : 'Add a New Person';

  return (
    <PageWrapper>
      <Stack spacing={3}>
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <PageTitle>{title}</PageTitle>
          <Button
            variant="contained"
            onClick={form.handleSubmit}
            disabled={form.isSubmitting}
          >
            {form.isSubmitting ? 'Saving...' : isEdit ? 'Save Changes' : 'Create Person'}
          </Button>
        </Stack>

        {form.submitError ? (
          <Alert severity="error">{form.submitError}</Alert>
        ) : null}

        <EntityForm
          fieldDefinitions={fieldDefinitions}
          values={form.values}
          errors={form.errors}
          onChange={form.setValue}
          groups={groups}
          renderCustomField={renderCustomField}
          peopleOptions={peopleOptions}
          disabled={form.isSubmitting}
        />

        <Stack direction="row" spacing={2} justifyContent="flex-end">
          <Button
            variant="contained"
            onClick={form.handleSubmit}
            disabled={form.isSubmitting}
          >
            {form.isSubmitting ? 'Saving...' : isEdit ? 'Save Changes' : 'Create Person'}
          </Button>
        </Stack>
      </Stack>
    </PageWrapper>
  );
};

export default PersonFormPage;
