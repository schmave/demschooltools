import React, { useCallback } from 'react';
import SingleOrMultiAutocomplete from './SingleOrMultiAutocomplete';
import Chip from './Chip';

const normalizeId = (value) => {
  if (value === null || value === undefined) {
    return '';
  }
  return String(value);
};

const listsAreEqual = (listA, listB) => {
  if (listA.length !== listB.length) {
    return false;
  }
  const idsA = listA.map((item) => normalizeId(item.id)).sort();
  const idsB = listB.map((item) => normalizeId(item.id)).sort();
  return idsA.every((id, index) => id === idsB[index]);
};

const getDisplayLabel = (option) => option?.label || option?.name || '';

const PeoplePicker = (props) => {
  const {
    label = 'People',
    placeholder = 'Search people',
    options = [],
    selectedPeople = [],
    onAddPerson,
    onRemovePerson,
    onSelectionChange,
    limitToOne = false,
    disabled = false,
    onPersonClick,
    size = 'medium',
    fullWidth = true,
    ...restProps
  } = props;

  const normalizedSelectedPeople = Array.isArray(selectedPeople)
    ? selectedPeople.filter(Boolean)
    : [];

  const handleChange = useCallback(
    (event, newValue, reason, details) => {
      let nextValue;

      if (limitToOne) {
        const option = Array.isArray(newValue)
          ? newValue[newValue.length - 1]
          : newValue;
        nextValue = option ? [option] : [];
      } else {
        nextValue = newValue || [];
      }

      const previousIds = new Set(
        normalizedSelectedPeople.map((person) => normalizeId(person.id)),
      );
      const nextIds = new Set(nextValue.map((person) => normalizeId(person.id)));

      if (onAddPerson) {
        nextValue.forEach((person) => {
          const personId = normalizeId(person.id);
          if (!previousIds.has(personId)) {
            onAddPerson(person);
          }
        });
      }

      if (onRemovePerson) {
        normalizedSelectedPeople.forEach((person) => {
          const personId = normalizeId(person.id);
          if (!nextIds.has(personId)) {
            onRemovePerson(person);
          }
        });
      }

      if (
        onSelectionChange &&
        !listsAreEqual(normalizedSelectedPeople, nextValue)
      ) {
        onSelectionChange(nextValue);
      }
    },
    [
      limitToOne,
      onAddPerson,
      onRemovePerson,
      onSelectionChange,
      normalizedSelectedPeople,
    ],
  );

  return (
    <SingleOrMultiAutocomplete
      multiple={!limitToOne}
      options={options}
      value={limitToOne ? normalizedSelectedPeople[0] || null : normalizedSelectedPeople}
      onChange={handleChange}
      label={label}
      placeholder={placeholder}
      size={size}
      fullWidth={fullWidth}
      disabled={disabled}
      getOptionLabel={getDisplayLabel}
      renderTags={
        !limitToOne
          ? (tagValue, getTagProps) =>
              tagValue.map((option, index) => {
                const tagProps = getTagProps({ index });
                return (
                  <Chip
                    {...tagProps}
                    key={normalizeId(option.id) || index}
                    label={getDisplayLabel(option)}
                    onClick={
                      onPersonClick ? () => onPersonClick(option) : tagProps.onClick
                    }
                  />
                );
              })
          : undefined
      }
      isOptionEqualToValue={(option, selected) =>
        normalizeId(option?.id) === normalizeId(selected?.id)
      }
      {...restProps}
    />
  );
};

PeoplePicker.defaultProps = {
  selectedPeople: [],
  options: [],
};

export default PeoplePicker;
