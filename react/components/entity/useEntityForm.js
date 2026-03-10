import { useCallback, useState } from 'react';

/**
 * Hook for managing entity form state, validation, and submission.
 * Entity-agnostic — works for Person, Tag, Family, Company, etc.
 *
 * @param {Object} params
 * @param {Array} params.fieldDefinitions - unified field definitions
 * @param {Object} params.initialValues - initial values {key: value}
 * @param {Function} params.onSubmit - async (values) => void
 */
export const useEntityForm = ({ fieldDefinitions, initialValues, onSubmit }) => {
  const [values, setValues] = useState(() => ({ ...initialValues }));
  const [errors, setErrors] = useState({});
  const [isDirty, setIsDirty] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

  const setValue = useCallback((key, value) => {
    setValues((prev) => ({ ...prev, [key]: value }));
    setIsDirty(true);
    // Clear field-level error when user edits
    setErrors((prev) => {
      if (prev[key]) {
        const next = { ...prev };
        delete next[key];
        return next;
      }
      return prev;
    });
  }, []);

  const setMultipleValues = useCallback((valueMap) => {
    setValues((prev) => ({ ...prev, ...valueMap }));
    setIsDirty(true);
  }, []);

  const validate = useCallback(() => {
    const newErrors = {};

    for (const field of fieldDefinitions) {
      // Skip special fields (phone_numbers, tags, family) — validated elsewhere
      if (field.fieldType === 'special') continue;
      // Skip disabled/hidden custom fields
      if (!field.isCore && !field.enabled) continue;

      const val = values[field.key];

      // Required check
      if (field.required) {
        if (val === null || val === undefined || val === '' || (Array.isArray(val) && val.length === 0)) {
          newErrors[field.key] = `${field.label} is required.`;
          continue;
        }
      }

      // Skip further validation if value is empty and not required
      if (val === null || val === undefined || val === '' || (Array.isArray(val) && val.length === 0)) {
        continue;
      }

      const tv = field.typeValidation || {};

      // Type-specific validation
      if (field.fieldType === 'text') {
        if (typeof val === 'string') {
          if (tv.minLength != null && val.length < Number(tv.minLength)) {
            newErrors[field.key] = `Must be at least ${tv.minLength} characters.`;
          } else if (tv.maxLength != null && val.length > Number(tv.maxLength)) {
            newErrors[field.key] = `Must be at most ${tv.maxLength} characters.`;
          } else if (tv.requiredPattern) {
            try {
              const regex = new RegExp(tv.requiredPattern);
              if (!regex.test(val)) {
                newErrors[field.key] = tv.errorMessage || 'Invalid format.';
              }
            } catch (_e) {
              // invalid regex — skip
            }
          }
        }
      }

      if (['integer', 'number', 'controlledNumber', 'currency'].includes(field.fieldType)) {
        const num = Number(val);
        if (isNaN(num)) {
          newErrors[field.key] = 'Must be a valid number.';
        } else {
          if (tv.min != null && num < Number(tv.min)) {
            newErrors[field.key] = `Must be at least ${tv.min}.`;
          } else if (tv.max != null && num > Number(tv.max)) {
            newErrors[field.key] = `Must be at most ${tv.max}.`;
          }
        }
      }

      if (['select', 'checkboxGroup', 'peopleSelect'].includes(field.fieldType)) {
        if (Array.isArray(val)) {
          if (tv.minSelected != null && val.length < Number(tv.minSelected)) {
            newErrors[field.key] = `Select at least ${tv.minSelected}.`;
          }
          if (tv.maxSelected != null && val.length > Number(tv.maxSelected)) {
            newErrors[field.key] = `Select at most ${tv.maxSelected}.`;
          }
        }
      }
    }

    setErrors(newErrors);
    return { isValid: Object.keys(newErrors).length === 0, errors: newErrors };
  }, [fieldDefinitions, values]);

  const handleSubmit = useCallback(
    async (event) => {
      if (event) event.preventDefault();
      setSubmitError(null);

      const { isValid } = validate();
      if (!isValid) return;

      setIsSubmitting(true);
      try {
        await onSubmit(values);
      } catch (err) {
        const message = extractErrorMessage(err);
        setSubmitError(message);

        // If the server returns field-level errors, set them
        if (err?.body && typeof err.body === 'object') {
          const serverErrors = {};
          for (const [key, val] of Object.entries(err.body)) {
            serverErrors[key] = Array.isArray(val) ? val.join(' ') : String(val);
          }
          if (Object.keys(serverErrors).length > 0) {
            setErrors((prev) => ({ ...prev, ...serverErrors }));
          }
        }
      } finally {
        setIsSubmitting(false);
      }
    },
    [validate, values, onSubmit],
  );

  const reset = useCallback(
    (newValues) => {
      setValues(newValues ? { ...newValues } : { ...initialValues });
      setErrors({});
      setIsDirty(false);
      setSubmitError(null);
    },
    [initialValues],
  );

  return {
    values,
    errors,
    isDirty,
    isSubmitting,
    submitError,
    setValue,
    setMultipleValues,
    validate,
    handleSubmit,
    reset,
    setErrors,
  };
};

const extractErrorMessage = (error) => {
  if (error?.body) {
    if (typeof error.body === 'string') return error.body;
    if (typeof error.body === 'object') {
      const firstKey = Object.keys(error.body)[0];
      const value = error.body[firstKey];
      if (Array.isArray(value)) return value.join(' ');
      if (typeof value === 'string') return value;
    }
  }
  return error?.message || 'Something went wrong.';
};
