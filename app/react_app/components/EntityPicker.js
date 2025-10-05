import React, { useCallback } from 'react';
import SingleOrMultiAutocomplete from './SingleOrMultiAutocomplete';
import Chip from './Chip';

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

  const normalizedValue = multiple
    ? (Array.isArray(value) ? value : []).filter(Boolean)
    : value || null;

  const handleChange = useCallback(
    (event, newValue) => {
      if (!onChange) {
        return;
      }
      if (multiple) {
        onChange(Array.isArray(newValue) ? newValue.filter(Boolean) : []);
      } else {
        onChange(newValue || null);
      }
    },
    [multiple, onChange],
  );

  const renderTags = multiple
    ? (tagValue, getTagProps) =>
        tagValue.map((option, index) => {
          const tagProps = getTagProps({ index });
          return (
            <Chip
              {...tagProps}
              key={normalizeId(option.id) || index}
              label={getOptionLabel(option)}
              onClick={
                onOptionClick ? () => onOptionClick(option) : tagProps.onClick
              }
            />
          );
        })
    : undefined;

  return (
    <SingleOrMultiAutocomplete
      multiple={multiple}
      options={options}
      value={normalizedValue}
      onChange={handleChange}
      label={label}
      placeholder={placeholder}
      size={size}
      fullWidth={fullWidth}
      disabled={disabled}
      getOptionLabel={getOptionLabel}
      renderTags={renderTags}
      isOptionEqualToValue={(option, selected) =>
        normalizeId(option?.id) === normalizeId(selected?.id)
      }
      {...restProps}
    />
  );
};

export default EntityPicker;
