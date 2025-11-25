import React, { useCallback, useMemo } from 'react';
import {
  InputLabel,
  MenuItem,
  FormControl,
  Select,
  Autocomplete,
  Chip,
  useTheme,
  Box,
} from '@mui/material';
import { CgClose } from 'react-icons/cg';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import CheckBoxIcon from '@mui/icons-material/CheckBox';
import Divider from './Divider';
// Prefer local wrappers for consistency
import TextField from './TextField';
import IconButton from './IconButton';
import Checkbox from './Checkbox';

// Unified Select + Autocomplete component
// Supports both single/multiple, primitive value arrays (value) and option metadata
// Backwards compatibility: accepts setValue (legacy) OR onChange (MUI-like) handlers
const SelectInput = (props) => {
  const {
    value: rawValue,
    setValue, // legacy
    onChange: externalOnChange, // preferred
    label,
    fullWidth = true,
    options = [],
    disabled,
    marginTop = '10px',
    marginBottom = '10px',
    showOptionCount = false,
    showClearButton = false,
    clearButtonLabel = 'Clear Selection',
    autocomplete = false,
    multiple = false,
    placeholder = '',
    size = 'small',
    startAdornment,
    endAdornment,
    ...restOfProps
  } = props;
  const theme = useTheme();
  const icon = <CheckBoxOutlineBlankIcon fontSize="small" />;
  const checkedIcon = <CheckBoxIcon fontSize="small" />;

  // Normalize incoming external value for internal logic
  const value = useMemo(() => {
    if (multiple) {
      if (Array.isArray(rawValue)) return rawValue;
      return [];
    }
    return rawValue === undefined || rawValue === null ? '' : rawValue;
  }, [rawValue, multiple]);

  // Map of primitive value -> option object
  const optionMap = useMemo(() => {
    const map = new Map();
    options.forEach((o) => map.set(o.value, o));
    return map;
  }, [options]);

  // Autocomplete expects option objects (or array) as value
  const autoValue = useMemo(() => {
    if (!autocomplete) return undefined;
    if (multiple) {
      if (!Array.isArray(value)) return [];
      return value.map((v) => optionMap.get(v)).filter(Boolean);
    }
    return value ? optionMap.get(value) || null : null;
  }, [autocomplete, multiple, value, optionMap]);

  const propagateValue = useCallback(
    (eventLike, newVal) => {
      if (setValue) setValue(newVal);
      if (externalOnChange) externalOnChange(eventLike, newVal);
    },
    [setValue, externalOnChange],
  );

  const handleSelectChange = (event) => {
    const newVal = event.target.value;
    if (showClearButton && newVal === 'clearSelectionOverride') {
      const cleared = multiple ? [] : '';
      propagateValue(event, cleared);
      return;
    }
    propagateValue(event, newVal);
  };

  const handleAutoChange = useCallback(
    (event, newValue) => {
      if (multiple) {
        const mapped = Array.isArray(newValue)
          ? newValue.map((o) => o?.value).filter((v) => v !== undefined)
          : [];
        propagateValue(event, mapped);
      } else {
        const mapped = newValue ? newValue.value : '';
        propagateValue(event, mapped);
      }
    },
    [multiple, propagateValue],
  );

  const hasValue = multiple
    ? Array.isArray(value) && value.length > 0
    : value !== undefined && value !== null && value !== '';

  const handleClear = useCallback(
    (eventLike) => {
      const cleared = multiple ? [] : '';
      propagateValue(eventLike, cleared);
    },
    [multiple, propagateValue],
  );

  const isClearVisible = showClearButton && hasValue;

  if (autocomplete) {
    const {
      componentsProps: incomingComponentsProps,
      clearIcon: incomingClearIcon,
      disableClearable: incomingDisableClearable,
      ...autocompleteRestProps
    } = restOfProps;

    const clearIndicatorSx = showClearButton
      ? {
          color: theme.palette.primary.main,
          visibility: isClearVisible ? 'visible' : 'hidden',
          opacity: isClearVisible ? 1 : 0,
          transition: 'opacity 0.2s',
        }
      : {};

    const componentsProps = {
      ...incomingComponentsProps,
      clearIndicator: {
        ...(incomingComponentsProps?.clearIndicator || {}),
        sx: {
          ...(incomingComponentsProps?.clearIndicator?.sx || {}),
          ...clearIndicatorSx,
        },
      },
    };

    const clearIcon = showClearButton
      ? <CgClose color={theme.palette.primary.main} />
      : incomingClearIcon;

    const finalDisableClearable =
      typeof incomingDisableClearable === 'boolean'
        ? incomingDisableClearable
        : showClearButton
        ? false
        : undefined;

    return (
      <FormControl fullWidth={fullWidth} sx={{ marginTop, marginBottom }}>
        <Autocomplete
          {...autocompleteRestProps}
          multiple={multiple}
          disableCloseOnSelect={multiple}
            // Provide full option objects
          options={options}
          value={autoValue}
          onChange={handleAutoChange}
          size={size}
          disabled={disabled || options.length === 0}
          componentsProps={componentsProps}
          clearIcon={clearIcon}
          disableClearable={finalDisableClearable}
          getOptionLabel={(option) => {
            if (!option) return '';
            const base = option.label || '';
            return showOptionCount && typeof option.count === 'number' ? `${base} (${option.count})` : base;
          }}
          isOptionEqualToValue={(opt, val) => opt.value === val.value}
          renderOption={(renderProps, option, { selected }) => {
            const { key, ...optionProps } = renderProps;
            const disabledStyle = showOptionCount && option.count === 0 ? { color: theme.palette.text.disabled } : {};
            return (
              <li key={key} {...optionProps} style={disabledStyle}>
                {multiple && (
                  <Checkbox
                    icon={icon}
                    checkedIcon={checkedIcon}
                    style={{ marginRight: 8 }}
                    checked={selected}
                    disabled={showOptionCount && option.count === 0}
                  />
                )}
                {option.leftIcon && (
                  <span style={{ display: 'inline-flex', marginRight: 4 }}>{option.leftIcon}</span>
                )}
                <span style={{ flexGrow: 1 }}>
                  {option.label}
                  {showOptionCount && typeof option.count === 'number' && ` (${option.count})`}
                </span>
                {option.rightIcon && (
                  <span style={{ display: 'inline-flex', marginLeft: 4 }}>{option.rightIcon}</span>
                )}
              </li>
            );
          }}
          renderTags={(tagValue, getTagProps) =>
            tagValue.map((option, index) => {
              const tagProps = getTagProps({ index });
              return (
                <Chip
                  {...tagProps}
                  key={option.value}
                  label={option.label}
                  size="small"
                />
              );
            })
          }
          renderInput={(params) => (
            <TextField
              {...params}
              label={label}
              placeholder={placeholder}
              InputProps={{
                ...params.InputProps,
                startAdornment: startAdornment && hasValue ? (
                  <>
                    {startAdornment}
                    {params.InputProps.startAdornment}
                  </>
                ) : params.InputProps.startAdornment,
                endAdornment: (
                  <>
                    {endAdornment}
                    {params.InputProps.endAdornment}
                  </>
                ),
              }}
            />
          )}
        />
      </FormControl>
    );
  }

  // Fallback: Select behavior
  return (
    <FormControl fullWidth={fullWidth} sx={{ marginTop, marginBottom }}>
      {label && <InputLabel id="select-label">{label}</InputLabel>}
      <Select
        value={value}
        label={label}
        onChange={handleSelectChange}
        disabled={disabled || options.length === 0}
        multiple={multiple}
        size={size}
        endAdornment={
          showClearButton && isClearVisible ? (
            <IconButton
              size="small"
              sx={{ mr: 0.5 }}
              onMouseDown={(e) => e.stopPropagation()}
              onClick={(e) => {
                e.stopPropagation();
                handleClear(e);
              }}
              aria-label={clearButtonLabel}
            >
              <CgClose color={theme.palette.primary.main} />
            </IconButton>
          ) : undefined
        }
        renderValue={
          multiple
            ? (selected) => {
                if (!Array.isArray(selected) || selected.length === 0) return '';
                const selectedOptions = options.filter((o) => selected.includes(o.value));
                return (
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {selectedOptions.map((o) => (
                      <Chip
                        key={o.value}
                        label={o.label}
                        size="small"
                        onMouseDown={(e) => e.stopPropagation()}
                        onDelete={(e) => {
                          e.stopPropagation();
                          const newSelected = selected.filter((v) => v !== o.value);
                          const syntheticEvent = { target: { value: newSelected } };
                          propagateValue(syntheticEvent, newSelected);
                        }}
                      />
                    ))}
                  </Box>
                );
              }
            : undefined
        }
        {...restOfProps}
      >
        {options.map((option) => {
          const disabledOption = showOptionCount && option.count === 0;
          return (
            <MenuItem
              key={option.value}
              value={option.value}
              sx={{ color: disabledOption ? theme.palette.text.disabled : 'auto' }}
              disabled={disabledOption}
            >
              {multiple && (
                <Checkbox
                  checked={Array.isArray(value) && value.includes(option.value)}
                  onChange={() => { /* handled by Select collective change */ }}
                  size="small"
                  style={{ marginRight: 8 }}
                />
              )}
              {option.leftIcon && (
                <span style={{ display: 'inline-flex', marginRight: 4 }}>{option.leftIcon}</span>
              )}
              {option.label}
              {showOptionCount && typeof option.count === 'number' && ` (${option.count})`}
              {option.rightIcon && (
                <span style={{ display: 'inline-flex', marginLeft: 4 }}>{option.rightIcon}</span>
              )}
            </MenuItem>
          );
        })}
      </Select>
    </FormControl>
  );
};

export default SelectInput;
