import dayjs from 'dayjs';

export const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'];

export const parseRosterFromInitialData = () => {
  if (typeof window === 'undefined') {
    return [];
  }
  const raw = window.initialData?.people;
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed
      .map((item, index) => ({
        id: item.id ?? `person-${index}`,
        name: item.label ?? item.name ?? '',
        role: item.role ?? item.type ?? 'student',
      }))
      .filter((entry) => entry.name);
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error('Failed to parse roster data', error);
    return [];
  }
};

const getNameTokens = (name) => {
  if (!name) {
    return { first: '', last: '' };
  }
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) {
    return { first: '', last: parts[0].toLowerCase() };
  }
  return {
    first: parts.slice(0, -1).join(' ').toLowerCase(),
    last: parts[parts.length - 1].toLowerCase(),
  };
};

export const sortPeopleByName = (a, b) => {
  const aParts = getNameTokens(a.name);
  const bParts = getNameTokens(b.name);
  if (aParts.last !== bParts.last) {
    return aParts.last.localeCompare(bParts.last);
  }
  return aParts.first.localeCompare(bParts.first);
};


export const getPdfFontSizeForName = (name) => {
  if (name.length <= 18) {
    return 10;
  }
  if (name.length <= 24) {
    return 9;
  }
  if (name.length <= 32) {
    return 8;
  }
  return 7;
};

export const buildWeekDates = (weekStart) =>
  DAYS.map((_, index) => weekStart.add(index, 'day'));

export const formatWeekLabel = (weekDates) =>
  `${weekDates[0].format('MMM D')} - ${weekDates[4].format('MMM D, YYYY')}`;

export const formatGeneratedAt = () =>
  dayjs().format('MMMM D, YYYY h:mm A');

export const splitRosterByRole = (roster) => {
  const result = {
    students: [],
    guests: [],
    staff: [],
  };

  roster.forEach((person) => {
    switch (person.role) {
      case 'guest':
        result.guests.push(person);
        break;
      case 'staff':
        result.staff.push(person);
        break;
      default:
        result.students.push(person);
        break;
    }
  });

  return result;
};

export const assignDisplayIndices = (students, guests, staff) => {
  let counter = 1;
  const withStudents = students.map((person) => ({
    ...person,
    displayIndex: counter++,
  }));
  const withGuests = guests.map((person) => ({
    ...person,
    displayIndex: counter++,
  }));
  const withStaff = staff.map((person) => ({
    ...person,
    displayIndex: counter++,
  }));

  return {
    students: withStudents,
    guests: withGuests,
    staff: withStaff,
  };
};

export const buildTableRows = (students, guests, staff) => {
  const rows = [];

  students.forEach((person) => rows.push(person));

  if (guests.length) {
    rows.push({ type: 'section', label: 'Guests & Volunteers' });
    guests.forEach((person) => rows.push(person));
  }

  if (staff.length) {
    rows.push({ type: 'section', label: 'Staff' });
    staff.forEach((person) => rows.push(person));
  }

  return rows;
};

export const buildGuestStaffRows = (guests, staff) => {
  const rows = [];

  if (guests.length) {
    rows.push({ type: 'section', label: 'Guests & Volunteers' });
    guests.forEach((person) => rows.push(person));
  }

  if (staff.length) {
    rows.push({ type: 'section', label: 'Staff' });
    staff.forEach((person) => rows.push(person));
  }

  return rows;
};
