/**
 * Utilities for building unified field definitions from core + custom field data.
 * These are entity-agnostic — used by Person, Tag, Family, Company, etc.
 */

/**
 * Convert a CustomField API object to the unified field definition shape.
 */
export const customFieldToDefinition = (cf) => ({
  key: `cf_${cf.id}`,
  label: cf.label,
  fieldType: cf.field_type,
  required: cf.required,
  displayOrder:
    cf.display_order === null || cf.display_order === undefined
      ? Number.MAX_SAFE_INTEGER
      : cf.display_order,
  isCore: false,
  helpText: cf.help_text || '',
  typeProps: cf.type_props || {},
  typeValidation: cf.type_validation || {},
  defaultValue: cf.default_value ?? null,
  customFieldId: cf.id,
  groupId: cf.group || null,
  disabled: cf.disabled,
  enabled: cf.enabled,
  visibleToRoleIds: cf.visible_to_role_ids || [],
  editableByRoleIds: cf.editable_by_role_ids || [],
});

/**
 * Merge core field definitions and custom field definitions,
 * sort by displayOrder then label.
 */
export const buildFieldDefinitions = (coreFields, customFields = []) => {
  const customDefs = customFields.filter((cf) => cf.enabled).map(customFieldToDefinition);

  return [...coreFields, ...customDefs].sort((a, b) => {
    const orderA = a.displayOrder ?? Number.MAX_SAFE_INTEGER;
    const orderB = b.displayOrder ?? Number.MAX_SAFE_INTEGER;
    if (orderA !== orderB) return orderA - orderB;
    return a.label.localeCompare(b.label);
  });
};

/**
 * Build initial values map from field definitions and optional existing values.
 * For edit mode, existingValues contains {key: value} for core fields
 * and customFieldValues contains {cf_id: value} for custom fields.
 */
export const getInitialValues = (fieldDefinitions, existingValues = {}, customFieldValues = {}) => {
  const values = {};
  const nullDefaultTypes = ['date', 'datetime', 'integer', 'number', 'controlledNumber', 'currency', 'toggle'];
  for (const field of fieldDefinitions) {
    if (field.isCore) {
      const raw = existingValues[field.key];
      values[field.key] = raw ?? (nullDefaultTypes.includes(field.fieldType) ? null : '');
    } else if (field.customFieldId) {
      values[field.key] =
        customFieldValues[String(field.customFieldId)] ?? field.defaultValue ?? null;
    }
  }
  return values;
};
