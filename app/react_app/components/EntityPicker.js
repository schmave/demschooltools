import React, { useCallback, useMemo } from 'react';
import SelectInput from './SelectInput';

const defaultGetOptionLabel = (option) => option?.label || option?.name || '';
const normalizeId = (value) => (value === null || value === undefined ? '' : String(value));

const EntityPicker = (props) => {
  const {
    label,
    placeholder,
    options = [],
    value,
    multiple = false,
    onChange,
    size = 'medium',
    disabled = false,
    getOptionLabel = defaultGetOptionLabel,
    onOptionClick,
    fullWidth = true,
    ...restProps
  } = props;

  const normalizedValue = useMemo(
    () => (multiple ? (Array.isArray(value) ? value.filter(Boolean) : []) : value || null),
    [value, multiple],
  );

  // Convert option objects to { value, label } format expected by SelectInput
  const mappedOptions = useMemo(
    () => options.map((o) => ({ ...o, value: normalizeId(o.id), label: getOptionLabel(o) })),
    [options, getOptionLabel],
  );

  // For new SelectInput autocomplete mode, we pass primitive ids
  const primitiveValue = useMemo(() => {
    if (multiple) {
      return Array.isArray(normalizedValue)
        ? normalizedValue.map((o) => normalizeId(o.id))
        : [];
    }
    return normalizedValue ? normalizeId(normalizedValue.id) : '';
  }, [normalizedValue, multiple]);

  const handlePrimitiveChange = useCallback(
    (event, newPrimitive) => {
      if (!onChange) return;
      if (multiple) {
        const nextObjects = Array.isArray(newPrimitive)
          ? newPrimitive
              .map((id) => options.find((o) => normalizeId(o.id) === normalizeId(id)))
              .filter(Boolean)
          : [];
        onChange(nextObjects);
      } else {
        const nextObj = options.find((o) => normalizeId(o.id) === normalizeId(newPrimitive)) || null;
        onChange(nextObj);
      }
    },
    [onChange, multiple, options],
  );

  return (
    <SelectInput
      autocomplete
      multiple={multiple}
      options={mappedOptions}
      value={primitiveValue}
      onChange={handlePrimitiveChange}
      label={label}
      placeholder={placeholder}
      size={size}
      fullWidth={fullWidth}
      disabled={disabled}
      {...restProps}
    />
  );
};

export default EntityPicker;
