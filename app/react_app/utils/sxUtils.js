export const combineSx = (...values) => {
  const entries = values.reduce((acc, value) => {
    if (value === null || value === undefined) {
      return acc;
    }
    if (Array.isArray(value)) {
      acc.push(...value);
    } else {
      acc.push(value);
    }
    return acc;
  }, []);

  if (entries.length === 0) {
    return undefined;
  }

  return entries.length === 1 ? entries[0] : entries;
};
