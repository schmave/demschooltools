import React, { useState } from 'react';
import Autocomplete from './Autocomplete';
import Chip from './Chip';
import TextField from './TextField';
const normalizeId = (value) => (value === null || value === undefined ? '' : String(value));

const SingleOrMultiAutocomplete = (props) => {
  const {
    multiple = false,
    options = [],
    value,
    onChange,
    label,
    placeholder,
    size = 'small',
    fullWidth = false,
    ChipComponent = Chip,
    renderValue,
    renderTags: renderTagsProp,
    renderOption: renderOptionProp,
    readOnly = false,
    getOptionLabel = (option) => option?.label || option?.name || '',
    isOptionEqualToValue = (option, selected) =>
      normalizeId(option?.id) === normalizeId(selected?.id),
    ...autocompleteProps
  } = props;

  const normalizedValue = multiple
    ? Array.isArray(value)
      ? value.filter(Boolean)
      : []
    : value || null;

  const [isFocused, setIsFocused] = useState(false);
  const hasValue = multiple
    ? Array.isArray(normalizedValue) && normalizedValue.length > 0
    : Boolean(normalizedValue);

  const renderInput = (params) => {
    const inputProps = {
      ...params.inputProps,
      placeholder: isFocused ? placeholder : '',
      onFocus: (event) => {
        params.inputProps?.onFocus?.(event);
        setIsFocused(true);
      },
      onBlur: (event) => {
        params.inputProps?.onBlur?.(event);
        setIsFocused(false);
      },
    };

    const InputProps = {
      ...params.InputProps,
      readOnly,
      startAdornment: hasValue ? params.InputProps?.startAdornment : null,
    };

    return (
      <TextField
        {...params}
        label={label}
        size={size}
        fullWidth={fullWidth}
        InputProps={InputProps}
        inputProps={inputProps}
      />
    );
  };

  return (
    <Autocomplete
      multiple={multiple}
      disableCloseOnSelect={multiple}
      options={options}
      value={normalizedValue}
      onChange={onChange}
      getOptionLabel={getOptionLabel}
      isOptionEqualToValue={isOptionEqualToValue}
      renderInput={renderInput}
      renderTags={
        renderTagsProp ??
        (multiple
          ? (tagValue, getTagProps) =>
              tagValue.map((option, index) => {
                const tagProps = getTagProps({ index });
                return (
                  <ChipComponent
                    {...tagProps}
                    key={normalizeId(option.id) || index}
                    label={getOptionLabel(option)}
                  />
                );
              })
          : undefined)
      }
      renderOption={
        renderOptionProp ??
        (renderValue && !multiple ? (optionProps, option) => renderValue(optionProps, option) : undefined)
      }
      clearOnBlur
      {...autocompleteProps}
    />
  );
};

export default SingleOrMultiAutocomplete;
