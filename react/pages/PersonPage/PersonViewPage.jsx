import React from 'react';
import { Chip } from '@mui/material';
import {
  Alert,
  Button,
  PageTitle,
  PageWrapper,
  Stack,
  Typography,
} from '../../components';
import {
  EntityForm,
  buildFieldDefinitions,
  getInitialValues,
} from '../../components/entity';
import { DeleteDialog } from '../../containers';
import { SnackbarContext } from '../../contexts';
import { safeParse } from '../../utils';
import PhoneNumbersInput from './PhoneNumbersInput';
import { deletePerson } from './api';
import { CORE_PERSON_FIELDS } from './personFields';

const getInitialData = () => {
  if (typeof window === 'undefined') return {};
  return window.initialData || {};
};

const PersonViewPage = () => {
  const initialData = getInitialData();
  const personApiUrl = initialData.personApiUrl || '/api/people/';
  const groups = safeParse(initialData.groups, []);
  const customFields = safeParse(initialData.customFields, []);
  const peopleOptions = safeParse(initialData.peopleOptions, []).map((p) => ({
    value: p.id,
    label: p.label,
  }));
  const showElectronicSignin = initialData.showElectronicSignin ?? false;

  const person = safeParse(initialData.person, null);
  const cfValues = safeParse(initialData.customFieldValues, {});
  const familyMembers = safeParse(initialData.familyMembers, []);

  const { setSnackbar } = React.useContext(SnackbarContext);
  const [deleteOpen, setDeleteOpen] = React.useState(false);

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

  const values = React.useMemo(() => {
    if (!person) return {};
    const vals = getInitialValues(fieldDefinitions, person, cfValues);
    vals.phone_numbers = person.phone_numbers || [];
    vals.tags = person.tags?.map((t) => t.id) || [];
    vals.family_person_id = person.family_person_id || null;
    return vals;
  }, [fieldDefinitions, person, cfValues]);

  const handleDelete = async () => {
    if (!person?.id) return;
    try {
      await deletePerson(personApiUrl, person.id);
      setSnackbar({ message: 'Person deleted.' });
      window.location.href = '/people';
    } catch (_error) {
      setSnackbar({ message: 'Failed to delete person.', severity: 'error' });
    } finally {
      setDeleteOpen(false);
    }
  };

  if (!person) {
    return (
      <PageWrapper>
        <Alert severity="error">Person not found.</Alert>
      </PageWrapper>
    );
  }

  const displayName = person.display_name || person.first_name;
  const fullName = `${displayName} ${person.last_name}`.trim();

  // Custom renderers for special fields in read-only mode
  const renderCustomField = (key, fieldDef, value) => {
    if (key === 'phone_numbers') {
      return <PhoneNumbersInput phones={person.phone_numbers || []} readOnly />;
    }
    if (key === 'tags') {
      const tags = person.tags || [];
      if (tags.length === 0) return null;
      return (
        <Stack spacing={0.5}>
          <Typography variant="caption" color="text.secondary">
            Tags
          </Typography>
          <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
            {tags.map((tag) => (
              <Chip key={tag.id} label={tag.title} size="small" />
            ))}
          </Stack>
        </Stack>
      );
    }
    if (key === 'family_person_id') {
      if (!value && familyMembers.length === 0) return null;
      const familyPerson = peopleOptions.find((p) => p.value === value);
      return (
        <Stack spacing={0.5}>
          <Typography variant="caption" color="text.secondary">
            Family
          </Typography>
          {familyPerson ? (
            <Typography variant="body1">
              Same family as:{' '}
              <a href={`/people/${familyPerson.value}`}>{familyPerson.label}</a>
            </Typography>
          ) : null}
          {familyMembers.length > 0 ? (
            <Stack spacing={0.25}>
              <Typography variant="body2" color="text.secondary">
                Family members:
              </Typography>
              {familyMembers.map((fm) => (
                <Typography key={fm.id} variant="body1">
                  <a href={`/people/${fm.id}`}>{fm.label}</a>
                </Typography>
              ))}
            </Stack>
          ) : null}
        </Stack>
      );
    }
    return null;
  };

  return (
    <PageWrapper>
      <Stack spacing={3}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <PageTitle>{fullName}</PageTitle>
          <Stack direction="row" spacing={1}>
            <Button
              variant="contained"
              onClick={() => {
                window.location.href = `/people/edit/${person.id}`;
              }}
            >
              Edit
            </Button>
            <Button
              variant="outlined"
              color="error"
              onClick={() => setDeleteOpen(true)}
            >
              Delete
            </Button>
          </Stack>
        </Stack>

        <EntityForm
          fieldDefinitions={fieldDefinitions}
          values={values}
          groups={groups}
          renderCustomField={renderCustomField}
          readOnly
          peopleOptions={peopleOptions}
          onChange={() => {}}
        />
      </Stack>

      <DeleteDialog
        open={deleteOpen}
        title="Delete person"
        message={`Are you sure you want to delete ${fullName}? This cannot be undone.`}
        handleConfirm={handleDelete}
        handleClose={() => setDeleteOpen(false)}
      />
    </PageWrapper>
  );
};

export default PersonViewPage;
