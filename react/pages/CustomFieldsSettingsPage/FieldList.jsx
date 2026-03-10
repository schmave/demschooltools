import React from 'react';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import {
  Chip,
  CircularProgress,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material';
import {
  DndContext,
  DragOverlay,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import {
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Box, Button, Paper, Tooltip, Typography } from '../../components';

const GROUP_PREFIX = 'group_';

const getFieldOrderKey = (field) =>
  field.isCore ? field.id : `cf_${field.id}`;

/**
 * Draggable field row.
 */
const SortableFieldRow = ({ field, onEdit, onDelete, onToggleEnabled }) => {
  const orderKey = getFieldOrderKey(field);
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: orderKey });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.4 : field.disabled ? 0.6 : 1,
  };

  return (
    <TableRow ref={setNodeRef} style={style}>
      <TableCell sx={{ width: 36, px: 0.5 }}>
        <Box sx={{ cursor: 'grab', display: 'flex', color: 'text.disabled' }} {...attributes} {...listeners}>
          <DragIndicatorIcon fontSize="small" />
        </Box>
      </TableCell>
      <TableCell sx={{ pl: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="body2" sx={{ fontWeight: field.isCore ? 600 : 400 }}>
            {field.label}
          </Typography>
          {field.isCore ? (
            <Chip size="small" label="Core" color="default" variant="outlined" />
          ) : null}
        </Box>
      </TableCell>
      <TableCell>
        <Typography variant="body2" color="text.secondary">
          {field.typeLabel}
        </Typography>
      </TableCell>
      <TableCell>{field.required ? 'Yes' : '—'}</TableCell>
      <TableCell>
        {field.isCore && field.required ? (
          <Typography variant="body2" color="text.secondary">
            Always on
          </Typography>
        ) : field.disabled ? (
          <Chip size="small" label="Locked" color="warning" variant="outlined" />
        ) : (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Switch
              size="small"
              checked={field.enabled !== false}
              onChange={() => onToggleEnabled(field)}
            />
            <Typography variant="body2">
              {field.enabled !== false ? 'On' : 'Off'}
            </Typography>
          </Box>
        )}
      </TableCell>
      <TableCell align="right">
        {!field.isCore && (
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 0.5 }}>
            <Button size="small" onClick={() => onEdit(field)}>
              Edit
            </Button>
            <Button
              size="small"
              color="error"
              variant="outlined"
              onClick={() => onDelete(field)}
              disabled={field.disabled}
            >
              Delete
            </Button>
          </Box>
        )}
      </TableCell>
    </TableRow>
  );
};

/**
 * Draggable group header row (drags the whole group).
 */
const SortableGroupHeader = ({ group, hasFields, onEditGroup, onDeleteGroup }) => {
  const sortableId = `${GROUP_PREFIX}${group.id}`;
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: sortableId });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <TableRow ref={setNodeRef} style={style} sx={{ backgroundColor: 'action.hover' }}>
      <TableCell sx={{ width: 36, px: 0.5 }}>
        <Box sx={{ cursor: 'grab', display: 'flex', color: 'text.disabled' }} {...attributes} {...listeners}>
          <DragIndicatorIcon fontSize="small" />
        </Box>
      </TableCell>
      <TableCell colSpan={4}>
        <Typography variant="body2" sx={{ fontWeight: 700 }}>
          {group.label}
        </Typography>
      </TableCell>
      <TableCell align="right">
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 0.5 }}>
          <Button size="small" onClick={() => onEditGroup(group)}>
            Edit
          </Button>
          {hasFields ? (
            <Tooltip title="Remove all fields from this group first">
              <span>
                <Button size="small" color="error" variant="outlined" disabled>
                  Delete
                </Button>
              </span>
            </Tooltip>
          ) : (
            <Button
              size="small"
              color="error"
              variant="outlined"
              onClick={() => onDeleteGroup(group)}
            >
              Delete
            </Button>
          )}
        </Box>
      </TableCell>
    </TableRow>
  );
};

const UnifiedFieldTable = ({
  groups,
  fields,
  loading,
  onEdit,
  onDelete,
  onToggleEnabled,
  onEditGroup,
  onDeleteGroup,
  onDragReorder,
  onDragReorderGroup,
}) => {
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor),
  );

  const sortedGroups = React.useMemo(
    () => [...groups].sort((a, b) => (a.display_order ?? 0) - (b.display_order ?? 0)),
    [groups],
  );

  const groupSortableIds = React.useMemo(
    () => sortedGroups.map((g) => `${GROUP_PREFIX}${g.id}`),
    [sortedGroups],
  );

  // Build field lookup and field lists per group
  const fieldsByGroup = React.useMemo(() => {
    const fieldByOrderKey = {};
    fields.forEach((f) => {
      if (f.isCore) {
        fieldByOrderKey[f.id] = f;
      } else {
        fieldByOrderKey[`cf_${f.id}`] = f;
        fieldByOrderKey[f.id] = f;
      }
    });

    const byGroup = {};
    sortedGroups.forEach((g) => {
      const orderedKeys = g.core_field_keys || [];
      const orderedFields = orderedKeys.map((key) => fieldByOrderKey[key]).filter(Boolean);
      const orderedKeySet = new Set(orderedKeys);
      const unorderedCustom = fields
        .filter((f) => !f.isCore && f.group === g.id && !orderedKeySet.has(`cf_${f.id}`))
        .sort((a, b) => {
          const orderA = a.display_order ?? Number.MAX_SAFE_INTEGER;
          const orderB = b.display_order ?? Number.MAX_SAFE_INTEGER;
          if (orderA !== orderB) return orderA - orderB;
          return a.label.localeCompare(b.label);
        });
      byGroup[g.id] = [...orderedFields, ...unorderedCustom];
    });

    return byGroup;
  }, [sortedGroups, fields]);

  // Map field order key -> group id
  const keyToGroupId = React.useMemo(() => {
    const map = {};
    sortedGroups.forEach((g) => {
      (fieldsByGroup[g.id] || []).forEach((f) => {
        map[getFieldOrderKey(f)] = g.id;
      });
    });
    return map;
  }, [sortedGroups, fieldsByGroup]);

  // Live drag state for cross-group smoothness
  const [activeId, setActiveId] = React.useState(null);
  const [liveGroupFields, setLiveGroupFields] = React.useState(null);
  const effectiveGroupFields = liveGroupFields || fieldsByGroup;

  const activeField = React.useMemo(() => {
    if (!activeId || activeId.startsWith(GROUP_PREFIX)) return null;
    for (const gId of Object.keys(fieldsByGroup)) {
      const found = fieldsByGroup[gId]?.find((f) => getFieldOrderKey(f) === activeId);
      if (found) return found;
    }
    return null;
  }, [activeId, fieldsByGroup]);

  const handleDragStart = ({ active }) => {
    setActiveId(String(active.id));
    const clone = {};
    sortedGroups.forEach((g) => {
      clone[g.id] = [...(fieldsByGroup[g.id] || [])];
    });
    setLiveGroupFields(clone);
  };

  const handleDragOver = ({ active, over }) => {
    if (!over || !liveGroupFields) return;
    const aId = String(active.id);
    const overId = String(over.id);

    if (aId.startsWith(GROUP_PREFIX) || overId.startsWith(GROUP_PREFIX)) return;

    const sourceGroupId = Object.keys(liveGroupFields).find((gId) =>
      liveGroupFields[gId].some((f) => getFieldOrderKey(f) === aId),
    );
    const targetGroupId = Object.keys(liveGroupFields).find((gId) =>
      liveGroupFields[gId].some((f) => getFieldOrderKey(f) === overId),
    );

    if (!sourceGroupId || !targetGroupId || sourceGroupId === targetGroupId) return;

    const sourceFields = [...liveGroupFields[sourceGroupId]];
    const targetFields = [...liveGroupFields[targetGroupId]];
    const fieldIdx = sourceFields.findIndex((f) => getFieldOrderKey(f) === aId);
    if (fieldIdx === -1) return;
    const [moved] = sourceFields.splice(fieldIdx, 1);
    const overIdx = targetFields.findIndex((f) => getFieldOrderKey(f) === overId);
    targetFields.splice(overIdx >= 0 ? overIdx : targetFields.length, 0, moved);

    setLiveGroupFields((prev) => ({
      ...prev,
      [Number(sourceGroupId)]: sourceFields,
      [Number(targetGroupId)]: targetFields,
    }));
  };

  const handleDragEnd = (event) => {
    const { active, over } = event;

    // Capture live state before clearing
    const finalGroupFields = liveGroupFields;
    setActiveId(null);
    setLiveGroupFields(null);

    if (!active || !over || active.id === over.id) return;

    const dragActiveId = String(active.id);
    const overId = String(over.id);

    // Group drag
    if (dragActiveId.startsWith(GROUP_PREFIX) && overId.startsWith(GROUP_PREFIX)) {
      const fromGroupId = Number(dragActiveId.replace(GROUP_PREFIX, ''));
      const toGroupId = Number(overId.replace(GROUP_PREFIX, ''));
      const fromIdx = sortedGroups.findIndex((g) => g.id === fromGroupId);
      const toIdx = sortedGroups.findIndex((g) => g.id === toGroupId);
      if (fromIdx === -1 || toIdx === -1 || fromIdx === toIdx) return;
      onDragReorderGroup(fromIdx, toIdx);
      return;
    }

    // Field drag
    if (!dragActiveId.startsWith(GROUP_PREFIX) && !overId.startsWith(GROUP_PREFIX)) {
      const sourceGroupId = keyToGroupId[dragActiveId];
      // Use live state to find target group (handles cross-group moves)
      let targetGroupId = keyToGroupId[overId];
      if (finalGroupFields) {
        const liveTarget = Object.keys(finalGroupFields).find((gId) =>
          finalGroupFields[gId].some((f) => getFieldOrderKey(f) === overId),
        );
        if (liveTarget) targetGroupId = Number(liveTarget);
      }

      if (!sourceGroupId || !targetGroupId) return;

      const targetFields = (finalGroupFields ? finalGroupFields[targetGroupId] : fieldsByGroup[targetGroupId]) || [];
      const overIndex = targetFields.findIndex((f) => getFieldOrderKey(f) === overId);

      onDragReorder(dragActiveId, sourceGroupId, targetGroupId, overIndex);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress size={32} />
      </Box>
    );
  }

  if (!groups.length && !fields.length) {
    return (
      <Typography variant="body1" color="text.secondary">
        No groups or fields configured yet.
      </Typography>
    );
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragStart={handleDragStart}
      onDragOver={handleDragOver}
      onDragEnd={handleDragEnd}
    >
      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ width: 36 }} />
              <TableCell>Label</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Required</TableCell>
              <TableCell>Visible</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <SortableContext items={groupSortableIds} strategy={verticalListSortingStrategy}>
              {sortedGroups.map((group) => {
                const groupFields = effectiveGroupFields[group.id] || [];
                const hasFields = groupFields.length > 0;
                const sortableIds = groupFields.map(getFieldOrderKey);

                return (
                  <React.Fragment key={`group-${group.id}`}>
                    <SortableGroupHeader
                      group={group}
                      hasFields={hasFields}
                      onEditGroup={onEditGroup}
                      onDeleteGroup={onDeleteGroup}
                    />

                    <SortableContext items={sortableIds} strategy={verticalListSortingStrategy}>
                      {groupFields.map((field) => (
                        <SortableFieldRow
                          key={getFieldOrderKey(field)}
                          field={field}
                          onEdit={onEdit}
                          onDelete={onDelete}
                          onToggleEnabled={onToggleEnabled}
                        />
                      ))}
                    </SortableContext>

                    {groupFields.length === 0 && (
                      <TableRow>
                        <TableCell />
                        <TableCell colSpan={5} sx={{ pl: 2 }}>
                          <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic' }}>
                            No fields in this group
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </React.Fragment>
                );
              })}
            </SortableContext>
          </TableBody>
        </Table>
      </TableContainer>
      <DragOverlay>
        {activeField ? (
          <Paper variant="outlined" sx={{ px: 2, py: 1, opacity: 0.9 }}>
            <Typography variant="body2">{activeField.label}</Typography>
          </Paper>
        ) : null}
      </DragOverlay>
    </DndContext>
  );
};

export default UnifiedFieldTable;
