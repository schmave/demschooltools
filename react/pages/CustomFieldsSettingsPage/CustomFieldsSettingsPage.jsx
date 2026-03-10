import React from 'react';
import { Button, Divider, PageTitle, PageWrapper, Stack, Typography } from '../../components';
import { DeleteDialog } from '../../containers';
import { SnackbarContext } from '../../contexts';
import { safeParse } from '../../utils';
import { CORE_FIELDS, ENTITY_TYPES, FIELD_TYPE_LABELS } from './constants';
import UnifiedFieldTable from './FieldList';
import CustomFieldModal from './CustomFieldModal';
import GroupModal from './GroupModal';
import {
  createCustomField,
  createGroup,
  deleteCustomField,
  deleteGroup,
  fetchCustomFields,
  fetchGroups,
  fetchRoleKeys,
  patchCustomField,
  patchGroup,
  updateCustomField,
  updateGroup,
} from './api';

const getEntityLabel = (entityType) =>
  ENTITY_TYPES.find((type) => type.id === entityType)?.label || entityType;

const formatFieldForDisplay = (field, entityLabel) => ({
  ...field,
  entityLabel,
  typeLabel: FIELD_TYPE_LABELS[field.field_type] || field.field_type,
  isCore: Boolean(field.isCore),
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
  const groupsApiBase = initialData.groupsApiBase || '/api/custom-field-groups/';
  const roleKeysApiUrl = initialData.roleKeysApiUrl || '/api/role-keys/';
  const initialTags = normalizeTagOptions(safeParse(initialData.tagOptions, []));

  const { setSnackbar } = React.useContext(SnackbarContext);
  const entityType = 'person';
  const [customFields, setCustomFields] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [roleOptions, setRoleOptions] = React.useState([]);
  const [modalState, setModalState] = React.useState({ open: false, field: null });
  const [modalError, setModalError] = React.useState('');
  const [saving, setSaving] = React.useState(false);
  const [deleteTarget, setDeleteTarget] = React.useState(null);
  const [groups, setGroups] = React.useState([]);
  const [groupModalState, setGroupModalState] = React.useState({ open: false, group: null });
  const [groupModalError, setGroupModalError] = React.useState('');
  const [groupSaving, setGroupSaving] = React.useState(false);
  const [groupDeleteTarget, setGroupDeleteTarget] = React.useState(null);

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
    Promise.all([
      fetchCustomFields(customFieldsApiBase, entityType),
      fetchGroups(groupsApiBase, entityType),
    ])
      .then(([fieldsPayload, groupsPayload]) => {
        if (!isMounted) return;
        setCustomFields(fieldsPayload);
        setGroups(groupsPayload);
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
  }, [customFieldsApiBase, groupsApiBase, entityType, setSnackbar]);

  const entityLabel = getEntityLabel(entityType);
  const coreFields = React.useMemo(() => {
    // Build lookups from groups
    const keyToGroup = {};
    const hiddenKeys = new Set();
    for (const g of groups) {
      for (const k of g.core_field_keys || []) {
        keyToGroup[k] = g.id;
      }
      for (const k of g.hidden_core_field_keys || []) {
        hiddenKeys.add(k);
      }
    }
    return (CORE_FIELDS[entityType] || []).map((coreField) =>
      formatFieldForDisplay(
        {
          id: coreField.key,
          label: coreField.label,
          entity_type: entityType,
          field_type: coreField.fieldType,
          required: coreField.required,
          enabled: !hiddenKeys.has(coreField.key),
          disabled: false,
          display_order: coreField.displayOrder,
          isCore: true,
          group: keyToGroup[coreField.key] || null,
        },
        entityLabel,
      ),
    );
  }, [entityType, entityLabel, groups]);

  const fieldsForDisplay = React.useMemo(() => {
    const customEntries = customFields.map((field) =>
      formatFieldForDisplay(field, entityLabel),
    );
    return [...coreFields, ...customEntries];
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
        // Add to the group's field order
        if (created.group) {
          const cfKey = `cf_${created.id}`;
          const group = groups.find((g) => g.id === created.group);
          if (group) {
            const newKeys = [...(group.core_field_keys || []), cfKey];
            setGroups((prev) =>
              prev.map((g) =>
                g.id === created.group ? { ...g, core_field_keys: newKeys } : g,
              ),
            );
            patchGroup(groupsApiBase, created.group, { core_field_keys: newKeys }).catch(
              () => {},
            );
          }
        }
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
      // Toggle core field visibility via hidden_core_field_keys on the group
      if (field.required) return;
      const groupId = getFieldGroupId(field);
      if (!groupId) return;
      const group = groups.find((g) => g.id === groupId);
      if (!group) return;

      const hidden = group.hidden_core_field_keys || [];
      const isHidden = hidden.includes(field.id);
      const newHidden = isHidden
        ? hidden.filter((k) => k !== field.id)
        : [...hidden, field.id];

      const prevGroups = groups;
      setGroups((prev) =>
        prev.map((g) =>
          g.id === groupId ? { ...g, hidden_core_field_keys: newHidden } : g,
        ),
      );
      try {
        await patchGroup(groupsApiBase, groupId, { hidden_core_field_keys: newHidden });
      } catch (_error) {
        setGroups(prevGroups);
        setSnackbar({ message: 'Unable to update field visibility.', severity: 'error' });
      }
      return;
    }
    try {
      const updated = await patchCustomField(customFieldsApiBase, field.id, {
        enabled: !field.enabled,
      });
      setCustomFields((prev) =>
        prev.map((item) => (Number(item.id) === Number(updated.id) ? updated : item)),
      );
    } catch (_error) {
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
    } catch (_error) {
      setSnackbar({ message: 'Failed to delete field.', severity: 'error' });
    } finally {
      setDeleteTarget(null);
    }
  };

  const handleCloseGroupModal = () => {
    setGroupModalState({ open: false, group: null });
    setGroupModalError('');
    setGroupSaving(false);
  };

  const handleSubmitGroup = async (payload) => {
    setGroupSaving(true);
    setGroupModalError('');
    try {
      if (payload.id) {
        const updated = await updateGroup(groupsApiBase, payload.id, payload);
        setGroups((prev) =>
          prev.map((g) => (Number(g.id) === Number(updated.id) ? updated : g)),
        );
        setSnackbar({ message: 'Group updated successfully.' });
      } else {
        const created = await createGroup(groupsApiBase, payload);
        setGroups((prev) => [...prev, created]);
        setSnackbar({ message: 'Group created successfully.' });
      }
      handleCloseGroupModal();
    } catch (error) {
      setGroupModalError(extractErrorMessage(error));
    } finally {
      setGroupSaving(false);
    }
  };

  const handleDeleteGroupConfirmed = async () => {
    if (!groupDeleteTarget) return;
    try {
      await deleteGroup(groupsApiBase, groupDeleteTarget.id);
      setGroups((prev) =>
        prev.filter((g) => Number(g.id) !== Number(groupDeleteTarget.id)),
      );
      setSnackbar({ message: 'Group deleted.' });
    } catch (_error) {
      setSnackbar({ message: 'Failed to delete group.', severity: 'error' });
    } finally {
      setGroupDeleteTarget(null);
    }
  };

  // Build a lookup: field id -> group id (for both core and custom fields)
  const getFieldGroupId = React.useCallback(
    (field) => {
      if (field.isCore) {
        for (const g of groups) {
          if ((g.core_field_keys || []).includes(field.id)) return g.id;
        }
        return null;
      }
      return field.group;
    },
    [groups],
  );

  const handleDragReorderGroup = async (fromIdx, toIdx) => {
    const sorted = [...groups].sort(
      (a, b) => (a.display_order ?? 0) - (b.display_order ?? 0),
    );
    if (fromIdx < 0 || toIdx < 0 || fromIdx >= sorted.length || toIdx >= sorted.length) return;

    // Reorder the array and reassign display_order values
    const reordered = [...sorted];
    const [moved] = reordered.splice(fromIdx, 1);
    reordered.splice(toIdx, 0, moved);

    const updates = {};
    reordered.forEach((g, idx) => {
      const newOrder = (idx + 1) * 1000;
      if (g.display_order !== newOrder) {
        updates[g.id] = newOrder;
      }
    });

    if (Object.keys(updates).length === 0) return;

    const previousGroups = groups;
    setGroups((prev) =>
      prev.map((g) => (updates[g.id] != null ? { ...g, display_order: updates[g.id] } : g)),
    );

    try {
      await Promise.all(
        Object.entries(updates).map(([id, order]) =>
          patchGroup(groupsApiBase, id, { display_order: order }),
        ),
      );
    } catch (_error) {
      setGroups(previousGroups);
      setSnackbar({ message: 'Failed to reorder groups.', severity: 'error' });
    }
  };

  const handleDragReorder = async (fieldKey, sourceGroupId, targetGroupId, targetIndex) => {
    const prevGroups = groups;
    const prevFields = customFields;

    // Determine if the field is a custom field (needs group FK update)
    const isCustom = fieldKey.startsWith('cf_');
    const sameGroup = sourceGroupId === targetGroupId;

    if (sameGroup) {
      // Reorder within the same group's core_field_keys
      const group = groups.find((g) => g.id === sourceGroupId);
      if (!group) return;
      const keys = [...(group.core_field_keys || [])];
      const fromIdx = keys.indexOf(fieldKey);
      if (fromIdx === -1) return;
      keys.splice(fromIdx, 1);
      keys.splice(targetIndex, 0, fieldKey);

      setGroups((prev) =>
        prev.map((g) =>
          g.id === sourceGroupId ? { ...g, core_field_keys: keys } : g,
        ),
      );
      try {
        await patchGroup(groupsApiBase, sourceGroupId, { core_field_keys: keys });
      } catch (_error) {
        setGroups(prevGroups);
        setSnackbar({ message: 'Failed to reorder field.', severity: 'error' });
      }
    } else {
      // Move between groups
      const sourceGroup = groups.find((g) => g.id === sourceGroupId);
      const targetGroup = groups.find((g) => g.id === targetGroupId);
      if (!sourceGroup || !targetGroup) return;

      const newSourceKeys = (sourceGroup.core_field_keys || []).filter((k) => k !== fieldKey);
      const newTargetKeys = [...(targetGroup.core_field_keys || [])];
      newTargetKeys.splice(targetIndex, 0, fieldKey);

      setGroups((prev) =>
        prev.map((g) => {
          if (g.id === sourceGroupId) return { ...g, core_field_keys: newSourceKeys };
          if (g.id === targetGroupId) return { ...g, core_field_keys: newTargetKeys };
          return g;
        }),
      );
      if (isCustom) {
        const rawId = fieldKey.replace('cf_', '');
        setCustomFields((prev) =>
          prev.map((item) =>
            String(item.id) === rawId ? { ...item, group: targetGroupId } : item,
          ),
        );
      }

      try {
        const promises = [
          patchGroup(groupsApiBase, sourceGroupId, { core_field_keys: newSourceKeys }),
          patchGroup(groupsApiBase, targetGroupId, { core_field_keys: newTargetKeys }),
        ];
        if (isCustom) {
          const rawId = fieldKey.replace('cf_', '');
          promises.push(patchCustomField(customFieldsApiBase, rawId, { group: targetGroupId }));
        }
        await Promise.all(promises);
      } catch (_error) {
        setGroups(prevGroups);
        if (isCustom) setCustomFields(prevFields);
        setSnackbar({ message: 'Failed to move field.', severity: 'error' });
      }
    }
  };

  return (
    <PageWrapper>
      <Stack spacing={3}>
        <PageTitle>Custom Fields</PageTitle>
        <Typography variant="body1" color="text.secondary">
          Configure additional fields for {entityLabel.toLowerCase()} records. Core fields can be
          hidden but not deleted.
        </Typography>

        <Divider />

        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            onClick={() => setGroupModalState({ open: true, group: null })}
          >
            Add Group
          </Button>
          <Button variant="contained" onClick={() => setModalState({ open: true, field: null })}>
            Add Field
          </Button>
        </Stack>

        <UnifiedFieldTable
          groups={groups}
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
          onEditGroup={(group) => setGroupModalState({ open: true, group })}
          onDeleteGroup={setGroupDeleteTarget}
          onDragReorderGroup={handleDragReorderGroup}
          onDragReorder={handleDragReorder}
        />
      </Stack>

      <CustomFieldModal
        open={modalState.open}
        mode={modalState.field ? 'edit' : 'create'}
        entityType={entityType}
        field={modalState.field}
        roleOptions={roleOptions}
        tagOptions={initialTags}
        groups={groups}
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

      <GroupModal
        open={groupModalState.open}
        mode={groupModalState.group ? 'edit' : 'create'}
        entityType={entityType}
        group={groupModalState.group}
        saving={groupSaving}
        serverError={groupModalError}
        onClose={handleCloseGroupModal}
        onSubmit={handleSubmitGroup}
      />

      <DeleteDialog
        open={Boolean(groupDeleteTarget)}
        title="Delete group"
        message="Deleting a group will ungroup any fields assigned to it. Continue?"
        handleConfirm={handleDeleteGroupConfirmed}
        handleClose={() => setGroupDeleteTarget(null)}
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
