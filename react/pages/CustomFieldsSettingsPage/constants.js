export const ENTITY_TYPES = [
  { id: 'person', label: 'Person' },
  { id: 'tag', label: 'Tags' },
];

const ORDER_SPACING = 1000;

export const CORE_FIELDS = {
  person: [
    {
      key: 'first_name',
      label: 'First Name',
      fieldType: 'text',
      required: true,
      displayOrder: ORDER_SPACING * 1,
    },
    {
      key: 'last_name',
      label: 'Last Name',
      fieldType: 'text',
      required: true,
      displayOrder: ORDER_SPACING * 2,
    },
    {
      key: 'preferred_name',
      label: 'Preferred Name',
      fieldType: 'text',
      required: false,
      displayOrder: ORDER_SPACING * 3,
    },
    {
      key: 'email',
      label: 'Email',
      fieldType: 'text',
      required: false,
      displayOrder: ORDER_SPACING * 4,
    },
    {
      key: 'phone_number',
      label: 'Phone Number',
      fieldType: 'text',
      required: false,
      displayOrder: ORDER_SPACING * 5,
    },
    {
      key: 'status',
      label: 'Enrollment Status',
      fieldType: 'select',
      required: false,
      displayOrder: ORDER_SPACING * 6,
    },
  ],
  tag: [
    {
      key: 'title',
      label: 'Tag Title',
      fieldType: 'text',
      required: true,
      displayOrder: ORDER_SPACING * 1,
    },
    {
      key: 'color',
      label: 'Color',
      fieldType: 'text',
      required: false,
      displayOrder: ORDER_SPACING * 2,
    },
    {
      key: 'description',
      label: 'Description',
      fieldType: 'text',
      required: false,
      displayOrder: ORDER_SPACING * 3,
    },
  ],
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

export const FIELD_TYPE_LABELS = FIELD_TYPE_OPTIONS.reduce((acc, option) => {
  acc[option.value] = option.label;
  return acc;
}, {});

export const CONDITION_PLACEHOLDER =
  '[{ "fieldKey": "someCoreFieldKey", "operator": "equals", "value": "other" }]';
