import React from 'react';
import { Button, PageTitle, PageWrapper, Stack, Typography } from '../../components';
import { DeleteDialog } from '../../containers';
import { SnackbarContext } from '../../contexts';
import { safeParse } from '../../utils';
import { CORE_FIELDS, ENTITY_TYPES, FIELD_TYPE_LABELS } from './constants';
import EntityTypeSelector from './EntityTypeSelector';
import FieldList from './FieldList';
import CustomFieldModal from './CustomFieldModal';
import {
  createCustomField,
  deleteCustomField,
  fetchCustomFields,
  fetchRoleKeys,
  patchCustomField,
  updateCustomField,
} from './api';

const getEntityLabel = (entityType) =>
  ENTITY_TYPES.find((type) => type.id === entityType)?.label || entityType;

const formatFieldForDisplay = (field, entityLabel) => ({
  ...field,
  entityLabel,
  typeLabel: FIELD_TYPE_LABELS[field.field_type] || field.field_type,
  isCore: Boolean(field.isCore),
  displayValue:
    field.display_order === undefined || field.display_order === null
      ? '—'
      : field.display_order,
});

const normalizeTagOptions = (options = []) =>
  options.map((option) => ({
    value: String(option.id),
    label: option.label,
  }));

const normalizeRoleOptions = (options = []) =>
  options.map((option) => ({
    value: option.id,
    label: option.label,
  }));

const getInitialData = () => {
  if (typeof window === 'undefined') {
    return {};
  }
  return window.initialData || {};
};

const CustomFieldsSettingsPage = () => {
  const initialData = getInitialData();
  const customFieldsApiBase = initialData.customFieldsApiBase || '/api/custom-fields/';
  const roleKeysApiUrl = initialData.roleKeysApiUrl || '/api/role-keys/';
  const initialTags = normalizeTagOptions(safeParse(initialData.tagOptions, []));

  const { setSnackbar } = React.useContext(SnackbarContext);
  const [entityType, setEntityType] = React.useState(ENTITY_TYPES[0].id);
  const [customFields, setCustomFields] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [roleOptions, setRoleOptions] = React.useState([]);
  const [modalState, setModalState] = React.useState({ open: false, field: null });
  const [modalError, setModalError] = React.useState('');
  const [saving, setSaving] = React.useState(false);
  const [deleteTarget, setDeleteTarget] = React.useState(null);

  React.useEffect(() => {
    let isMounted = true;
    fetchRoleKeys(roleKeysApiUrl)
      .then((payload) => {
        if (!isMounted) return;
        setRoleOptions(normalizeRoleOptions(payload.roles || []));
      })
      .catch(() => {
        setSnackbar({
          message: 'Failed to load role options.',
          severity: 'error',
        });
      });
    return () => {
      isMounted = false;
    };
  }, [roleKeysApiUrl, setSnackbar]);

  React.useEffect(() => {
    let isMounted = true;
    setLoading(true);
    fetchCustomFields(customFieldsApiBase, entityType)
      .then((payload) => {
        if (!isMounted) return;
        setCustomFields(payload);
      })
      .catch(() => {
        setSnackbar({
          message: 'Failed to load custom fields.',
          severity: 'error',
        });
      })
      .finally(() => {
        if (isMounted) {
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [customFieldsApiBase, entityType, setSnackbar]);

  const entityLabel = getEntityLabel(entityType);
  const coreFields = React.useMemo(
    () =>
      (CORE_FIELDS[entityType] || []).map((coreField) =>
        formatFieldForDisplay(
          {
            id: coreField.key,
            label: coreField.label,
            entity_type: entityType,
            field_type: coreField.fieldType,
            required: coreField.required,
            enabled: true,
            disabled: false,
            display_order: coreField.displayOrder,
            isCore: true,
          },
          entityLabel,
        ),
      ),
    [entityType, entityLabel],
  );

  const fieldsForDisplay = React.useMemo(() => {
    const customEntries = customFields.map((field) =>
      formatFieldForDisplay(field, entityLabel),
    );
    const combined = [...coreFields, ...customEntries];
    combined.sort((a, b) => {
      const orderA =
        a.display_order === undefined || a.display_order === null
          ? Number.MAX_SAFE_INTEGER
          : a.display_order;
      const orderB =
        b.display_order === undefined || b.display_order === null
          ? Number.MAX_SAFE_INTEGER
          : b.display_order;
      if (orderA !== orderB) {
        return orderA - orderB;
      }
      return a.label.localeCompare(b.label);
    });
    return combined;
  }, [coreFields, customFields, entityLabel]);

  const handleCloseModal = () => {
    setModalState({ open: false, field: null });
    setModalError('');
    setSaving(false);
  };

  const handleSubmitField = async (payload) => {
    setSaving(true);
    setModalError('');
    try {
      if (payload.id) {
        const updated = await updateCustomField(customFieldsApiBase, payload.id, payload);
        setCustomFields((prev) =>
          prev.map((field) => (Number(field.id) === Number(updated.id) ? updated : field)),
        );
        setSnackbar({ message: 'Field updated successfully.' });
      } else {
        const created = await createCustomField(customFieldsApiBase, payload);
        setCustomFields((prev) => [...prev, created]);
        setSnackbar({ message: 'Field created successfully.' });
      }
      handleCloseModal();
    } catch (error) {
      setModalError(extractErrorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const handleToggleEnabled = async (field) => {
    if (field.isCore) {
      return;
    }
    try {
      const updated = await patchCustomField(customFieldsApiBase, field.id, {
        enabled: !field.enabled,
      });
      setCustomFields((prev) =>
        prev.map((item) => (Number(item.id) === Number(updated.id) ? updated : item)),
      );
    } catch (error) {
      setSnackbar({
        message: 'Unable to update field status.',
        severity: 'error',
      });
    }
  };

  const handleDeleteConfirmed = async () => {
    if (!deleteTarget) {
      return;
    }
    try {
      const result = await deleteCustomField(customFieldsApiBase, deleteTarget.id);
      if (result.deleted) {
        setCustomFields((prev) =>
          prev.filter((field) => Number(field.id) !== Number(deleteTarget.id)),
        );
        setSnackbar({ message: 'Field deleted.' });
      } else if (result.field) {
        setCustomFields((prev) =>
          prev.map((field) =>
            Number(field.id) === Number(result.field.id) ? result.field : field,
          ),
        );
        setSnackbar({
          message: 'Field disabled because it already has values.',
          severity: 'info',
        });
      }
    } catch (error) {
      setSnackbar({ message: 'Failed to delete field.', severity: 'error' });
    } finally {
      setDeleteTarget(null);
    }
  };

  const handleReorder = async (field, direction) => {
    if (field.isCore) {
      return;
    }

    const currentIndex = fieldsForDisplay.findIndex(
      (item) => !item.isCore && Number(item.id) === Number(field.id),
    );
    if (currentIndex === -1) {
      return;
    }
    let targetIndex =
      direction === 'up' ? currentIndex - 1 : currentIndex + 1;
    targetIndex = Math.max(0, Math.min(fieldsForDisplay.length - 1, targetIndex));

    const working = [...fieldsForDisplay];
    const [moved] = working.splice(currentIndex, 1);
    working.splice(targetIndex, 0, moved);

    let lastValue = 0;
    const nextOrders = {};

    working.forEach((item) => {
      if (item.isCore) {
        lastValue =
          item.display_order === undefined || item.display_order === null
            ? lastValue
            : item.display_order;
      } else {
        lastValue = lastValue ? lastValue + 1 : 1;
        nextOrders[item.id] = lastValue;
      }
    });

    const changes = Object.entries(nextOrders).filter(([id, order]) => {
      const original = customFields.find((entry) => Number(entry.id) === Number(id));
      return original && Number(original.display_order ?? 0) !== order;
    });

    if (changes.length === 0) {
      return;
    }

    try {
      await Promise.all(
        changes.map(([id, order]) =>
          patchCustomField(customFieldsApiBase, id, { display_order: order }),
        ),
      );
      setCustomFields((prev) =>
        prev.map((item) => {
          const change = changes.find(([id]) => Number(id) === Number(item.id));
          return change ? { ...item, display_order: change[1] } : item;
        }),
      );
      setSnackbar({ message: 'Field order updated.' });
    } catch (error) {
      setSnackbar({ message: 'Failed to update order.', severity: 'error' });
    }
  };

  return (
    <PageWrapper>
      <Stack spacing={3}>
        <PageTitle>Custom Fields</PageTitle>
        <Typography variant="body1" color="text.secondary">
          Configure additional fields for {entityLabel.toLowerCase()} records. Core fields are
          always visible and cannot be edited.
        </Typography>

        <Stack spacing={2}>
          <EntityTypeSelector value={entityType} onChange={setEntityType} />
          <Stack direction="row" justifyContent="flex-end">
            <Button variant="contained" onClick={() => setModalState({ open: true, field: null })}>
              Add Field
            </Button>
          </Stack>
        </Stack>

        <FieldList
          fields={fieldsForDisplay}
          loading={loading}
          onEdit={(field) => {
            const actual = customFields.find(
              (entry) => Number(entry.id) === Number(field.id),
            );
            setModalState({ open: true, field: actual || null });
          }}
          onDelete={setDeleteTarget}
          onToggleEnabled={handleToggleEnabled}
          onReorder={handleReorder}
        />
      </Stack>

      <CustomFieldModal
        open={modalState.open}
        mode={modalState.field ? 'edit' : 'create'}
        entityType={entityType}
        field={modalState.field}
        roleOptions={roleOptions}
        tagOptions={initialTags}
        saving={saving}
        serverError={modalError}
        onClose={handleCloseModal}
        onSubmit={handleSubmitField}
      />

      <DeleteDialog
        open={Boolean(deleteTarget)}
        title="Delete custom field"
        message="Deleting a field removes the definition permanently if no values exist. Continue?"
        handleConfirm={handleDeleteConfirmed}
        handleClose={() => setDeleteTarget(null)}
      />
    </PageWrapper>
  );
};

const extractErrorMessage = (error) => {
  if (error?.body) {
    if (typeof error.body === 'string') {
      return error.body;
    }
    if (typeof error.body === 'object') {
      const firstKey = Object.keys(error.body)[0];
      const value = error.body[firstKey];
      if (Array.isArray(value)) {
        return value.join(' ');
      }
      if (typeof value === 'string') {
        return value;
      }
    }
  }
  return error?.message || 'Something went wrong.';
};

export default CustomFieldsSettingsPage;
