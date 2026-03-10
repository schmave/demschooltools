import { CORE_PERSON_FIELDS } from '../PersonPage/personFields';

export const ENTITY_TYPES = [
  { id: 'person', label: 'Person' },
  { id: 'tag', label: 'Tags' },
];

const ORDER_SPACING = 1000;

const CORE_TAG_FIELDS = [
  {
    key: 'title',
    label: 'Tag Title',
    fieldType: 'text',
    required: true,
    displayOrder: ORDER_SPACING * 1,
  },
  {
    key: 'use_student_display',
    label: 'Use Student Display Name',
    fieldType: 'toggle',
    required: false,
    displayOrder: ORDER_SPACING * 2,
  },
  {
    key: 'show_in_jc',
    label: 'Show in JC',
    fieldType: 'toggle',
    required: false,
    displayOrder: ORDER_SPACING * 3,
  },
  {
    key: 'show_in_attendance',
    label: 'Show in Attendance',
    fieldType: 'toggle',
    required: false,
    displayOrder: ORDER_SPACING * 4,
  },
  {
    key: 'show_in_menu',
    label: 'Show in Menu',
    fieldType: 'toggle',
    required: false,
    displayOrder: ORDER_SPACING * 5,
  },
  {
    key: 'show_in_account_balances',
    label: 'Show in Account Balances',
    fieldType: 'toggle',
    required: false,
    displayOrder: ORDER_SPACING * 6,
  },
  {
    key: 'show_in_roles',
    label: 'Show in Roles',
    fieldType: 'toggle',
    required: false,
    displayOrder: ORDER_SPACING * 7,
  },
];

export const CORE_FIELDS = {
  person: CORE_PERSON_FIELDS,
  tag: CORE_TAG_FIELDS,
};

export const FIELD_TYPE_OPTIONS = [
  { value: 'text', label: 'Text' },
  { value: 'integer', label: 'Integer' },
  { value: 'number', label: 'Number' },
  { value: 'controlledNumber', label: 'Controlled Number' },
  { value: 'currency', label: 'Currency' },
  { value: 'date', label: 'Date' },
  { value: 'datetime', label: 'Datetime' },
  { value: 'toggle', label: 'Toggle' },
  { value: 'radioGroup', label: 'Radio Group' },
  { value: 'checkboxGroup', label: 'Checkbox Group' },
  { value: 'select', label: 'Select' },
  { value: 'peopleSelect', label: 'People Select' },
];

export const FIELD_TYPE_LABELS = FIELD_TYPE_OPTIONS.reduce(
  (acc, option) => {
    acc[option.value] = option.label;
    return acc;
  },
  { special: 'Built-in' },
);

export const CONDITION_PLACEHOLDER =
  '[{ "fieldKey": "someCoreFieldKey", "operator": "equals", "value": "other" }]';
