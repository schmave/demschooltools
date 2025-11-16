export const safeParse = (value, fallback) => {
  if (value === undefined || value === null) {
    return fallback;
  }
  if (typeof value === 'string') {
    try {
      return JSON.parse(value);
    } catch (error) {
      console.error('Failed to parse JSON', error);
      return fallback;
    }
  }
  return value;
};

export const getDisplayName = (person) => {
  if (!person) {
    return '???';
  }
  return (
    person.displayName ||
    [person.firstName, person.lastName].filter(Boolean).join(' ') ||
    person.name ||
    person.label ||
    '???'
  );
};

export const formatPeopleList = (people) =>
  people && people.length > 0 ? people.map(getDisplayName).join(', ') : 'None';

export const formatMultilineText = (text) => (text ? text : '');

export const boolFromConfig = (value) => value === true || value === 'true';

export const getPeopleAtCase = (caseItem) =>
  caseItem?.people_at_case || caseItem?.peopleAtCase || [];

export const getPeopleByRole = (caseItem, roleId) =>
  getPeopleAtCase(caseItem)
    .filter((entry) => Number(entry.role) === Number(roleId))
    .map((entry) => entry.person)
    .filter(Boolean);

export const caseIsEmpty = (caseItem) => {
  if (!caseItem) {
    return true;
  }

  const charges = Array.isArray(caseItem.charges) ? caseItem.charges : [];
  const peopleAtCase = getPeopleAtCase(caseItem);

  const hasFindings = Boolean(caseItem.findings && caseItem.findings.trim());
  const hasLocation = Boolean(caseItem.location && caseItem.location.trim());
  const hasDate = Boolean(caseItem.date);
  const hasTime = Boolean(caseItem.time && caseItem.time.trim());

  return (
    !hasFindings &&
    !hasLocation &&
    !hasDate &&
    !hasTime &&
    peopleAtCase.length === 0 &&
    charges.length === 0
  );
};

export const formatCaseDateTime = (caseItem) => {
  const segments = [];
  if (caseItem?.date) {
    segments.push(caseItem.date);
  }
  if (caseItem?.time) {
    segments.push(caseItem.time);
  }
  return segments.join(', ');
};

export const formatLocation = (location) =>
  location && location.trim().length > 0 ? location : 'unknown';

export const formatPlea = (plea, config) => {
  if (!plea || plea === '<no plea>' || plea === 'Guilty') {
    return '';
  }
  if (plea === 'Not Guilty') {
    return (config.str_not_guilty || 'Not Guilty') + '.';
  }
  if (plea === 'No Contest') {
    return 'No Contest.';
  }
  if (plea === 'N/A') {
    return (config.str_na || 'N/A') + '.';
  }
  return `${plea}.`;
};

export const getRuleTitle = (charge) => {
  if (!charge) {
    return '';
  }
  if (charge.ruleTitle) {
    return charge.ruleTitle;
  }
  if (charge.rule) {
    const parts = [];
    if (charge.rule.number) {
      parts.push(charge.rule.number);
    }
    if (charge.rule.title) {
      parts.push(charge.rule.title);
    }
    if (parts.length) {
      return parts.join(' ');
    }
  }
  return '';
};

export const getResolutionPlanText = (charge) => {
  if (!charge) {
    return '';
  }
  if (charge.smDecision) {
    return charge.smDecision;
  }
  if (charge.referredToSm && !charge.resolutionPlan) {
    return '[Referred to School Meeting]';
  }
  return charge.resolutionPlan || '';
};

export const formatAwaitingMessage = (template, person, ruleTitle) => {
  const destination = 'School Meeting';
  let message =
    template ||
    `*** Awaiting ${destination} decision on charging ${person || '???'} with ${
      ruleTitle || ''
    }.`;

  const replacements = {
    destination,
    person: person || '???',
    rule: ruleTitle || '',
  };

  Object.entries(replacements).forEach(([key, value]) => {
    message = message.replace(new RegExp(`\\\{${key}\\\}`, 'g'), value);
  });

  const ordered = [destination, replacements.person, replacements.rule];
  ordered.forEach((value, index) => {
    message = message.replace(new RegExp(`\\\{${index}\\\}`, 'g'), value);
  });

  return message;
};

export const normalizeOption = (item) => {
  if (!item) {
    return null;
  }
  const id = item.id ?? item.personId ?? item.person_id ?? item.case_id;
  if (id === undefined || id === null) {
    return null;
  }
  const label =
    item.label ||
    item.name ||
    item.displayName ||
    item.caseNumber ||
    item.title ||
    '';
  return {
    ...item,
    id: String(id),
    label,
  };
};

export const buildOptionMap = (options = []) => {
  const map = new Map();
  options.forEach((option) => {
    if (option?.id !== undefined && option?.id !== null) {
      map.set(String(option.id), option);
    }
  });
  return map;
};
