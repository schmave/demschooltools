import React, { useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { DndContext, closestCenter, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import { SortableContext, useSortable, verticalListSortingStrategy, arrayMove } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import {
  MaterialReactTable,
  useMaterialReactTable,
  MRT_ToggleGlobalFilterButton,
  MRT_ShowHideColumnsButton,
  MRT_ToggleDensePaddingButton,
  MRT_ToggleFullScreenButton,
} from 'material-react-table';
import { Autocomplete, Box, Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, IconButton, InputLabel, Menu, MenuItem, Select, Tab, Tabs, TextField, Tooltip } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import ClearIcon from '@mui/icons-material/Clear';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import CheckBoxIcon from '@mui/icons-material/CheckBox';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import SortIcon from '@mui/icons-material/Sort';
import VisibilityIcon from '@mui/icons-material/Visibility';
import WrapTextIcon from '@mui/icons-material/WrapText';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { SnackbarContext } from '../../contexts';
import { formatPhoneNumber, safeParse } from '../../utils';
import { CORE_PERSON_FIELDS } from '../PersonPage/personFields';
import { fetchPeople } from './api';
import { exportToExcel } from './exportToExcel';
import { useGridViews } from './useGridViews';
import ViewsToolbar from './ViewsToolbar';

const getInitialData = () => {
  if (typeof window === 'undefined') return {};
  return window.initialData || {};
};


const getUniqueValues = (data, key) =>
  [...new Set(data.map((row) => row[key]).filter((v) => v != null && v !== ''))].sort();

import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';

const uncheckedIcon = <CheckBoxOutlineBlankIcon fontSize="small" />;
const checkedIcon = <CheckBoxIcon fontSize="small" />;

const parseDateFilter = (filterValue) => {
  if (!filterValue || typeof filterValue !== 'string') return ['', ''];
  const between = filterValue.match(/^Between (\S+) and (\S+)$/);
  if (between) return [between[1], between[2]];
  const after = filterValue.match(/^On (\S+) or later$/);
  if (after) return [after[1], ''];
  const before = filterValue.match(/^On or before (\S+)$/);
  if (before) return ['', before[1]];
  return ['', ''];
};

const buildDateFilter = (from, to) => {
  if (!from && !to) return undefined;
  // Encode as parseable string that also reads nicely in MRT tooltip
  if (from && to) return `Between ${from} and ${to}`;
  if (from) return `On ${from} or later`;
  if (to) return `On or before ${to}`;
  return undefined;
};

const DateRangeFilter = ({ column }) => {
  const [from, to] = parseDateFilter(column.getFilterValue());

  const handleChange = (index, newVal) => {
    const newFrom = index === 0 ? (newVal?.isValid() ? newVal.format('YYYY-MM-DD') : '') : from;
    const newTo = index === 1 ? (newVal?.isValid() ? newVal.format('YYYY-MM-DD') : '') : to;
    column.setFilterValue(buildDateFilter(newFrom, newTo));
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, p: 0.5, minWidth: 220 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        <DatePicker
          label="From (on or after)"
          value={from ? dayjs(from) : null}
          onChange={(val) => handleChange(0, val)}
          slotProps={{
            textField: { size: 'small', variant: 'standard', autoFocus: true, sx: { flex: 1 } },
          }}
        />
        {from && (
          <IconButton size="small" onClick={() => handleChange(0, null)} sx={{ mt: 1 }}>
            <ClearIcon fontSize="small" />
          </IconButton>
        )}
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        <DatePicker
          label="To (on or before)"
          value={to ? dayjs(to) : null}
          onChange={(val) => handleChange(1, val)}
          slotProps={{
            textField: { size: 'small', variant: 'standard', sx: { flex: 1 } },
          }}
        />
        {to && (
          <IconButton size="small" onClick={() => handleChange(1, null)} sx={{ mt: 1 }}>
            <ClearIcon fontSize="small" />
          </IconButton>
        )}
      </Box>
    </Box>
  );
};

const dateFilterFn = (row, columnId, filterValue) => {
  if (!filterValue) return true;
  const [from, to] = parseDateFilter(filterValue);
  if (!from && !to) return true;
  const cellValue = row.getValue(columnId);
  if (!cellValue) return false;
  const cellDate = String(cellValue).slice(0, 10); // YYYY-MM-DD
  if (from && cellDate < from) return false;
  if (to && cellDate > to) return false;
  return true;
};
dateFilterFn.autoRemove = (val) => !val;

const SortableRow = ({ id, sort, index, sortableColumns, usedIds, onChange, onRemove }) => {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id });
  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <Box ref={setNodeRef} style={style} sx={{ display: 'flex', gap: 1, alignItems: 'center', mb: 1.5 }}>
      <Box {...attributes} {...listeners} sx={{ cursor: 'grab', display: 'flex', alignItems: 'center' }}>
        <DragIndicatorIcon fontSize="small" color="action" />
      </Box>
      <FormControl size="small" sx={{ flex: 2 }}>
        <InputLabel>Column</InputLabel>
        <Select
          value={sort.id}
          label="Column"
          onChange={(e) => onChange(index, { ...sort, id: e.target.value })}
        >
          {sortableColumns.filter((col) => col.id === sort.id || !usedIds.has(col.id)).map((col) => (
            <MenuItem key={col.id} value={col.id}>{col.header}</MenuItem>
          ))}
        </Select>
      </FormControl>
      <FormControl size="small" sx={{ flex: 1 }}>
        <InputLabel>Direction</InputLabel>
        <Select
          value={sort.desc ? 'desc' : 'asc'}
          label="Direction"
          onChange={(e) => onChange(index, { ...sort, desc: e.target.value === 'desc' })}
        >
          <MenuItem value="asc">Ascending</MenuItem>
          <MenuItem value="desc">Descending</MenuItem>
        </Select>
      </FormControl>
      <IconButton size="small" onClick={() => onRemove(index)}>
        <DeleteIcon fontSize="small" />
      </IconButton>
    </Box>
  );
};

const SortDialog = ({ open, onClose, sorting, onChange, sortableColumns, onApply, onClear }) => {
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));
  const sortIds = sorting.map((_, i) => `sort-${i}`);

  const handleDragEnd = (event) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const oldIndex = sortIds.indexOf(active.id);
    const newIndex = sortIds.indexOf(over.id);
    onChange(arrayMove([...sorting], oldIndex, newIndex));
  };

  const handleChange = (index, updated) => {
    const next = [...sorting];
    next[index] = updated;
    onChange(next);
  };

  const handleRemove = (index) => {
    onChange(sorting.filter((_, i) => i !== index));
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Sort Configuration</DialogTitle>
      <DialogContent sx={{ pt: 3, overflow: 'visible' }}>
        <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
          <SortableContext items={sortIds} strategy={verticalListSortingStrategy}>
            {sorting.map((sort, index) => {
              const usedIds = new Set(sorting.map((s) => s.id));
              return (
                <SortableRow
                  key={sortIds[index]}
                  id={sortIds[index]}
                  sort={sort}
                  index={index}
                  sortableColumns={sortableColumns}
                  usedIds={usedIds}
                  onChange={handleChange}
                  onRemove={handleRemove}
                />
              );
            })}
          </SortableContext>
        </DndContext>
        <Button
          startIcon={<AddIcon />}
          size="small"
          disabled={sorting.length >= sortableColumns.length}
          onClick={() => {
            const usedIds = new Set(sorting.map((s) => s.id));
            const next = sortableColumns.find((c) => !usedIds.has(c.id));
            if (next) onChange([...sorting, { id: next.id, desc: false }]);
          }}
          sx={{ mt: 1 }}
        >
          Add sort level
        </Button>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClear}>Clear All</Button>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={() => onApply(sorting.filter((s) => s.id))}>
          Apply
        </Button>
      </DialogActions>
    </Dialog>
  );
};

const TEXT_MODES = [
  { value: 'contains', label: 'Contains' },
  { value: 'startsWith', label: 'Starts with' },
  { value: 'endsWith', label: 'Ends with' },
  { value: 'equals', label: 'Equals' },
  { value: 'notEquals', label: 'Does not equal' },
  { value: 'empty', label: 'Is empty' },
  { value: 'notEmpty', label: 'Is not empty' },
];

const parseTextFilter = (val) => {
  if (typeof val === 'string') {
    const match = val.match(/^__text__(contains|startsWith|endsWith|equals|notEquals|empty|notEmpty)__(.*)$/);
    if (match) return { mode: match[1], text: match[2] };
  }
  return null;
};

const MultiSelectFilter = ({ column, options }) => {
  const rawValue = column.getFilterValue();
  const existingText = parseTextFilter(rawValue);
  const [tab, setTab] = useState(existingText ? 1 : 0);
  const [textMode, setTextMode] = useState(existingText?.mode || 'contains');
  const [textValue, setTextValue] = useState(existingText?.text || '');
  const selectValue = Array.isArray(rawValue) ? rawValue : [];
  const inputRef = React.useRef(null);

  React.useEffect(() => {
    const timer = setTimeout(() => inputRef.current?.focus(), 50);
    return () => clearTimeout(timer);
  }, []);

  const noTextNeeded = textMode === 'empty' || textMode === 'notEmpty';

  const handleTextChange = (mode, text) => {
    setTextMode(mode);
    setTextValue(text);
    if (mode === 'empty' || mode === 'notEmpty') {
      column.setFilterValue(`__text__${mode}__`);
    } else {
      column.setFilterValue(text ? `__text__${mode}__${text}` : undefined);
    }
  };

  return (
    <Box sx={{ minWidth: 240 }}>
      <Tabs value={tab} onChange={(_, v) => { setTab(v); column.setFilterValue(undefined); setTextValue(''); }} variant="fullWidth" sx={{ minHeight: 32, mb: 1 }}>
        <Tab label="Select Values" sx={{ minHeight: 32, py: 0, fontSize: '0.75rem' }} />
        <Tab label="Text Search" sx={{ minHeight: 32, py: 0, fontSize: '0.75rem' }} />
      </Tabs>
      {tab === 0 ? (
        <Autocomplete
          multiple
          open
          size="small"
          options={options}
          disableCloseOnSelect
          value={selectValue}
          onChange={(_, newValue) => column.setFilterValue(newValue.length ? newValue : undefined)}
          renderOption={(props, option, { selected }) => {
            const { key, ...rest } = props;
            return (
              <li key={key} {...rest} style={{ ...rest.style, padding: '2px 8px' }}>
                <Checkbox icon={uncheckedIcon} checkedIcon={checkedIcon} checked={selected} size="small" sx={{ mr: 0.5, p: 0.25 }} />
                {option}
              </li>
            );
          }}
          renderInput={(params) => <TextField {...params} inputRef={inputRef} placeholder="Search..." variant="standard" />}
        />
      ) : (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, px: 0.5 }}>
          <FormControl size="small" fullWidth>
            <InputLabel>Mode</InputLabel>
            <Select value={textMode} label="Mode" onChange={(e) => handleTextChange(e.target.value, textValue)}>
              {TEXT_MODES.map((m) => <MenuItem key={m.value} value={m.value}>{m.label}</MenuItem>)}
            </Select>
          </FormControl>
          {!noTextNeeded && (
            <TextField
              inputRef={inputRef}
              size="small"
              placeholder="Type to filter..."
              variant="standard"
              value={textValue}
              onChange={(e) => handleTextChange(textMode, e.target.value)}
              autoFocus
            />
          )}
        </Box>
      )}
      {rawValue && (
        <Button
          size="small"
          color="error"
          startIcon={<ClearIcon />}
          onClick={() => { column.setFilterValue(undefined); setTextValue(''); }}
          sx={{ mt: 1, alignSelf: 'flex-start', ml: 0.5 }}
        >
          Clear Filter
        </Button>
      )}
    </Box>
  );
};

const withMultiSelectFilter = (col, options) => {
  col.filterVariant = undefined;
  col.Filter = ({ column }) => <MultiSelectFilter column={column} options={options} />;
  col.filterFn = 'multiSelect';
  col.enableColumnFilterModes = false;
  return col;
};

const coreFieldToColumn = (field, data) => {
  const col = {
    accessorKey: field.key,
    header: field.label,
    size: 150,
  };

  switch (field.fieldType) {
    case 'date':
      col.sortingFn = 'datetime';
      col.Filter = ({ column }) => <DateRangeFilter column={column} />;
      col.filterFn = 'dateRange';
      col.enableColumnFilterModes = false;
      break;
    case 'select': {
      const opts = (field.typeProps?.options || []).map((o) => o.label || o.id);
      withMultiSelectFilter(col, opts);
      break;
    }
    default:
      withMultiSelectFilter(col, getUniqueValues(data, field.key));
      break;
  }

  return col;
};

const customFieldToColumn = (cf, data) => {
  const cfId = String(cf.id);
  const col = {
    id: `cf_${cfId}`,
    header: cf.label,
    accessorFn: (row) => {
      const vals = row.custom_field_values || {};
      return vals[cfId] ?? '';
    },
    size: 150,
  };

  switch (cf.field_type) {
    case 'date':
    case 'datetime':
      col.sortingFn = 'datetime';
      col.Filter = ({ column }) => <DateRangeFilter column={column} />;
      col.filterFn = 'dateRange';
      col.enableColumnFilterModes = false;
      break;
    case 'integer':
    case 'number':
    case 'controlledNumber':
    case 'currency':
      col.filterVariant = 'range';
      col.columnFilterModeOptions = ['between', 'greaterThan', 'lessThan', 'equals'];
      break;
    case 'toggle':
      col.filterVariant = 'checkbox';
      col.Cell = ({ cell }) => (cell.getValue() ? 'Yes' : 'No');
      break;
    case 'select':
    case 'radioGroup': {
      const options = cf.type_props?.options || [];
      const opts = options.map((o) => typeof o === 'string' ? o : (o.label || o.id || o.value));
      withMultiSelectFilter(col, opts);
      break;
    }
    default: {
      const uniqueVals = [...new Set(data.map((row) => (row.custom_field_values || {})[cfId]).filter((v) => v != null && v !== ''))].sort().map(String);
      withMultiSelectFilter(col, uniqueVals);
      break;
    }
  }

  return col;
};

function PeopleListPage() {
  const initialData = getInitialData();
  const customFields = safeParse(initialData.customFields, []);
  const currentUserId = initialData.currentUserId;

  const { setSnackbar } = useContext(SnackbarContext);

  const {
    viewList,
    activeViewId,
    saveView,
    loadView,
    deleteView,
    defaultViewState,
  } = useGridViews();

  const [people, setPeople] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [exportMenuAnchor, setExportMenuAnchor] = useState(null);
  const [sortDialogOpen, setSortDialogOpen] = useState(false);
  const [pendingSorting, setPendingSorting] = useState([]);

  // Controlled grid state (for save/restore views)
  const [columnVisibility, setColumnVisibility] = useState(defaultViewState.columnVisibility);
  const [columnOrder, setColumnOrder] = useState(defaultViewState.columnOrder);
  const [columnSizing, setColumnSizing] = useState(defaultViewState.columnSizing);
  const [sorting, setSorting] = useState(defaultViewState.sorting);
  const [columnFilters, setColumnFilters] = useState(defaultViewState.columnFilters);
  const [columnPinning, setColumnPinning] = useState(defaultViewState.columnPinning);
  const [density, setDensity] = useState(defaultViewState.density);
  const [globalFilter, setGlobalFilter] = useState(defaultViewState.globalFilter);
  const [wrapText, setWrapText] = useState(defaultViewState.wrapText);

  useEffect(() => {
    let cancelled = false;
    fetchPeople()
      .then((data) => {
        if (!cancelled) {
          setPeople(Array.isArray(data) ? data : data.results || []);
          setIsLoading(false);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.message || 'Failed to load people');
          setIsLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const getCurrentGridState = useCallback(
    () => ({
      columnVisibility,
      columnOrder,
      columnSizing,
      sorting,
      columnFilters,
      columnPinning,
      density,
      globalFilter,
      wrapText,
    }),
    [columnVisibility, columnOrder, columnSizing, sorting, columnFilters, columnPinning, density, globalFilter, wrapText],
  );

  const applyViewState = useCallback((viewState) => {
    setColumnVisibility(viewState.columnVisibility ?? {});
    setColumnOrder(viewState.columnOrder ?? []);
    setColumnSizing(viewState.columnSizing ?? {});
    setSorting(viewState.sorting ?? []);
    setColumnFilters(viewState.columnFilters ?? []);
    setColumnPinning(viewState.columnPinning ?? { left: [], right: [] });
    setDensity(viewState.density ?? 'compact');
    setGlobalFilter(viewState.globalFilter ?? '');
    setWrapText(viewState.wrapText ?? false);
  }, []);

  const handleSaveView = useCallback(
    async (name, isPrivate) => {
      try {
        await saveView(name, getCurrentGridState(), isPrivate);
        setSnackbar({ message: 'View saved', severity: 'success', duration: 3000 });
      } catch (err) {
        setSnackbar({ message: err.message || 'Failed to save view', severity: 'error', duration: 5000 });
      }
    },
    [saveView, getCurrentGridState, setSnackbar],
  );

  const handleLoadView = useCallback(
    (id) => {
      if (id === '__default__') {
        const viewState = loadView('__default__');
        if (viewState) applyViewState(viewState);
        window.history.replaceState(null, '', '/people/list');
        return;
      }
      const viewState = loadView(id);
      if (viewState) {
        applyViewState(viewState);
        window.history.replaceState(null, '', `/people/list?view=${id}`);
      }
    },
    [loadView, applyViewState],
  );

  const handleDeleteView = useCallback(
    async (id) => {
      try {
        await deleteView(id);
        if (id === activeViewId) {
          window.history.replaceState(null, '', '/people/list');
        }
        setSnackbar({ message: 'View deleted', severity: 'success', duration: 3000 });
      } catch (err) {
        setSnackbar({ message: err.message || 'Failed to delete view', severity: 'error', duration: 5000 });
      }
    },
    [deleteView, activeViewId, setSnackbar],
  );

  const columns = useMemo(() => {
    const actionsCol = {
      id: 'actions',
      header: 'Actions',
      size: 80,
      enableSorting: false,
      enableColumnFilter: false,
      enableHiding: false,
      enableColumnOrdering: false,
      Cell: ({ row }) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.25 }}>
          <a href={`/people/${row.original.id}`}>
            <IconButton size="small" color="default">
              <VisibilityIcon fontSize="small" />
            </IconButton>
          </a>
          <a href={`/people/edit/${row.original.id}`}>
            <IconButton size="small" color="default">
              <EditIcon fontSize="small" />
            </IconButton>
          </a>
        </Box>
      ),
    };

    const skipKeys = new Set(['phone_numbers', 'tags', 'family_person_id', 'pin']);
    const coreCols = CORE_PERSON_FIELDS.filter((f) => !skipKeys.has(f.key)).map((f) => coreFieldToColumn(f, people));

    const uniquePhones = [...new Set(people.flatMap((row) =>
      (row.phone_numbers || []).map((p) => formatPhoneNumber(p.number)).filter(Boolean),
    ))].sort();

    const phonesCol = withMultiSelectFilter({
      id: 'phone_numbers',
      header: 'Phone Numbers',
      accessorFn: (row) =>
        (row.phone_numbers || []).map((p) => formatPhoneNumber(p.number)).filter(Boolean).join(', '),
      size: 180,
    }, uniquePhones);
    phonesCol.filterFn = 'phoneMultiSelect';

    // Build unique tag names for multi-select filter
    const uniqueTags = [...new Set(people.flatMap((p) => (p.tags || []).map((t) => t.title)).filter(Boolean))].sort();

    const tagsCol = withMultiSelectFilter({
      id: 'tags',
      header: 'Tags',
      accessorFn: (row) =>
        (row.tags || []).map((t) => t.title).join(', '),
      size: 180,
    }, uniqueTags);
    tagsCol.filterFn = 'tagMultiSelect';

    const enabledCfs = customFields.filter((cf) => cf.enabled);
    const cfCols = enabledCfs.map((cf) => customFieldToColumn(cf, people));

    return [actionsCol, ...coreCols, phonesCol, tagsCol, ...cfCols];
  }, [customFields, people]);

  const applyTextMode = (mode, text, cellValue) => {
    if (mode === 'empty') return cellValue == null || String(cellValue).trim() === '';
    if (mode === 'notEmpty') return cellValue != null && String(cellValue).trim() !== '';
    if (!text) return true;
    if (cellValue == null) return false;
    const cell = String(cellValue).toLowerCase();
    const search = text.toLowerCase();
    switch (mode) {
      case 'contains': return cell.includes(search);
      case 'startsWith': return cell.startsWith(search);
      case 'endsWith': return cell.endsWith(search);
      case 'equals': return cell === search;
      case 'notEquals': return cell !== search;
      default: return true;
    }
  };

  const table = useMaterialReactTable({
    columns,
    data: people,
    filterFns: {
      dateRange: dateFilterFn,
      multiSelect: (row, columnId, filterValue) => {
        if (!filterValue) return true;
        const cellValue = row.getValue(columnId);
        if (typeof filterValue === 'string') {
          const parsed = filterValue.match(/^__text__(contains|startsWith|endsWith|equals|notEquals|empty|notEmpty)__(.*)$/);
          if (parsed) return applyTextMode(parsed[1], parsed[2], cellValue);
          return true;
        }
        if (!Array.isArray(filterValue) || filterValue.length === 0) return true;
        if (cellValue == null) return false;
        return filterValue.some((v) => String(cellValue).includes(v));
      },
      phoneMultiSelect: (row, columnId, filterValue) => {
        if (!filterValue) return true;
        // Get raw digits for each phone number
        const rawDigits = (row.original.phone_numbers || [])
          .map((p) => (p.number || '').replace(/\D/g, ''))
          .filter(Boolean);
        if (typeof filterValue === 'string') {
          const parsed = filterValue.match(/^__text__(contains|startsWith|endsWith|equals|notEquals|empty|notEmpty)__(.*)$/);
          if (parsed) {
            const [, mode, text] = parsed;
            if (mode === 'empty') return rawDigits.length === 0;
            if (mode === 'notEmpty') return rawDigits.length > 0;
            if (!text) return true;
            const searchDigits = text.replace(/\D/g, '');
            return rawDigits.some((digits) => applyTextMode(mode, searchDigits, digits));
          }
          return true;
        }
        if (!Array.isArray(filterValue) || filterValue.length === 0) return true;
        const formattedPhones = (row.original.phone_numbers || []).map((p) => formatPhoneNumber(p.number));
        return filterValue.some((phone) => formattedPhones.includes(phone));
      },
      tagMultiSelect: (row, columnId, filterValue) => {
        if (!filterValue) return true;
        const cellText = (row.original.tags || []).map((t) => t.title).join(', ');
        if (typeof filterValue === 'string') {
          const parsed = filterValue.match(/^__text__(contains|startsWith|endsWith|equals|notEquals|empty|notEmpty)__(.*)$/);
          if (parsed) return applyTextMode(parsed[1], parsed[2], cellText);
          return true;
        }
        if (!Array.isArray(filterValue) || filterValue.length === 0) return true;
        const rowTags = (row.original.tags || []).map((t) => t.title);
        return filterValue.some((tag) => rowTags.includes(tag));
      },
    },
    localization: {
      filterDateRange: '',
      filterMultiSelect: 'Includes',
      filterPhoneMultiSelect: 'Includes',
      filterTagMultiSelect: 'Includes',
    },
    // Features
    enableRowSelection: true,
    displayColumnDefOptions: {
      'mrt-row-select': { size: 40, grow: false },
    },
    enableColumnFilters: true,
    enableColumnOrdering: true,
    enableColumnResizing: true,
    enableSorting: true,
    enableColumnPinning: true,
    enableGlobalFilter: true,
    enableDensityToggle: true,
    enableFullScreenToggle: true,
    enableHiding: true,
    enablePagination: false,
    enableStickyHeader: true,
    enableBottomToolbar: false,
    columnFilterDisplayMode: 'popover',
    // Controlled state
    state: {
      isLoading,
      columnVisibility,
      columnOrder,
      columnSizing,
      sorting,
      columnFilters,
      columnPinning,
      density,
      globalFilter,
    },
    onColumnVisibilityChange: setColumnVisibility,
    onColumnOrderChange: setColumnOrder,
    onColumnSizingChange: setColumnSizing,
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    onColumnPinningChange: setColumnPinning,
    onDensityChange: setDensity,
    onGlobalFilterChange: setGlobalFilter,
    // Initial pagination (not saved in views)
    initialState: {
      showGlobalFilter: true,
    },
    // Styling
    muiTablePaperProps: {
      elevation: 0,
      sx: {
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        borderRadius: 0,
        border: 'none',
      },
    },
    muiTableContainerProps: {
      sx: { flex: 1 },
    },
    muiTableBodyCellProps: {
      sx: wrapText
        ? { whiteSpace: 'normal', wordBreak: 'break-word' }
        : { whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 300 },
    },
    muiTableBodyProps: {
      sx: {
        '& tr:nth-of-type(odd) > td': {
          backgroundColor: 'rgba(0, 0, 0, 0.02)',
        },
        '& tr:hover > td': {
          backgroundColor: 'rgba(0, 0, 0, 0.04)',
        },
      },
    },
    // Toolbar customization
    renderTopToolbarCustomActions: ({ table: tbl }) => {
      const hasSelection = tbl.getSelectedRowModel().rows.length > 0;
      const hasFilters = columnFilters.length > 0 || (globalFilter && globalFilter.length > 0);
      const allLabel = hasFilters ? 'Export All Filtered Rows' : 'Export All Rows';
      const selectedLabel = hasFilters ? 'Export Selected Filtered Rows' : 'Export Selected Rows';
      return (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Button
            variant="text"
            size="small"
            startIcon={<FileDownloadIcon />}
            onClick={(e) => {
              if (hasSelection) {
                setExportMenuAnchor(e.currentTarget);
              } else {
                exportToExcel(tbl);
              }
            }}
          >
            Export
          </Button>
          <Menu
            anchorEl={exportMenuAnchor}
            open={Boolean(exportMenuAnchor)}
            onClose={() => setExportMenuAnchor(null)}
          >
            <MenuItem
              onClick={() => {
                exportToExcel(tbl);
                setExportMenuAnchor(null);
              }}
            >
              {allLabel}
            </MenuItem>
            <MenuItem
              onClick={() => {
                exportToExcel(tbl, { selectedOnly: true });
                setExportMenuAnchor(null);
              }}
            >
              {selectedLabel}
            </MenuItem>
          </Menu>
        </Box>
      );
    },
    renderToolbarInternalActions: ({ table: tbl }) => (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: '2px' }}>
        <ViewsToolbar
          viewList={viewList}
          activeViewId={activeViewId}
          currentUserId={currentUserId}
          onSave={handleSaveView}
          onLoad={handleLoadView}
          onDelete={handleDeleteView}
          onShareFeedback={() => setSnackbar({ message: 'Link copied to clipboard!', severity: 'success', duration: 3000 })}
        />
        <Tooltip title="Multi-sort">
          <IconButton
            onClick={() => { setPendingSorting([...sorting]); setSortDialogOpen(true); }}
            color={sorting.length > 0 ? 'primary' : 'default'}
            size="small"
          >
            <SortIcon />
          </IconButton>
        </Tooltip>
        <Tooltip title={wrapText ? 'Disable text wrapping' : 'Enable text wrapping'}>
          <IconButton
            onClick={() => setWrapText((prev) => !prev)}
            color={wrapText ? 'primary' : 'default'}
            size="small"
          >
            <WrapTextIcon />
          </IconButton>
        </Tooltip>
        <MRT_ToggleGlobalFilterButton table={tbl} />
        <MRT_ShowHideColumnsButton table={tbl} />
        <MRT_ToggleDensePaddingButton table={tbl} />
        <MRT_ToggleFullScreenButton table={tbl} />
      </Box>
    ),
  });

  if (error) {
    return (
      <Box sx={{ p: 2, color: 'error.main' }}>Error: {error}</Box>
    );
  }

  const sortableColumns = columns
    .filter((c) => c.enableSorting !== false && c.id !== 'actions')
    .map((c) => ({ id: c.accessorKey || c.id, header: c.header }));

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 90px)', overflow: 'hidden' }}>
      <LocalizationProvider dateAdapter={AdapterDayjs}>
        <MaterialReactTable table={table} />
      </LocalizationProvider>

      <SortDialog
        open={sortDialogOpen}
        onClose={() => setSortDialogOpen(false)}
        sorting={pendingSorting}
        onChange={setPendingSorting}
        sortableColumns={sortableColumns}
        onApply={(newSorting) => { setSorting(newSorting); setSortDialogOpen(false); }}
        onClear={() => { setSorting([]); setSortDialogOpen(false); }}
      />
    </Box>
  );
}

export default PeopleListPage;
