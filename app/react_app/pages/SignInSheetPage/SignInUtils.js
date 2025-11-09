import dayjs from 'dayjs';

export const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'];

const SAMPLE_STUDENT_NAMES = [
  'Aaron Lee',
  'Jancey Rietmulder-Stone',
  'Stephenson Von Kuyk-White',
  'Abigail Parker',
  'Aiden Thompson',
  'Alexa Martin',
  'Amelia-Kate Thornton',
  'Andrew Chen',
  'Aria Brooks',
  'Avery Johnson',
  'Bella Stone',
  'Brayden Cooper',
  'Brooklyn Davis',
  'Caleb Wright',
  'Camila Sanders',
  'Carson Patel',
  'Charlotte Nguyen',
  'Chloe-Anne Rivers',
  'Cole Ramirez',
  'Daniel Foster',
  'Delaney Fox',
  'Eleanor Hayes',
  'Elijah Moore',
  "Emery O'Brien",
  'Emilia Hart',
  'Ethan Carter',
  'Evelyn Price',
  'Gabriel Young',
  'Grace Miller',
  'Grayson Scott',
  'Hadley Evans',
  'Harper James',
  'Henry Phillips',
  'Isabella Turner',
  'Jackson Reed',
  'Jasmine Brooks',
  'Jayden Clark',
  'Jonah Sullivan',
  'Josephine Blake',
  'Julian Ross',
  'Kaitlyn Harper',
  'Landon Brooks',
  'Lila Montgomery',
  'Logan Pierce',
  'Mason Bennett',
  'Mia Delgado',
  'Noah Gallagher',
  'Olivia Shaw',
  'Rowan Mitchell',
  'Samantha Ortiz',
];

const SAMPLE_GUEST_NAMES = [
  'Amira Guest',
  'Carlos Volunteer',
  'Jordan Guest',
  'Riley Volunteer',
];

const SAMPLE_STAFF_NAMES = [
  'Allison Mercer',
  'Brent Holloway',
  'Caroline Diaz',
  'Derrick Vaughn',
  'Felicia Grant',
  'Graham Whitaker',
];

export const buildSampleRoster = () => {
  const students = SAMPLE_STUDENT_NAMES.map((name, index) => ({
    id: `sample-student-${index}`,
    name,
    role: 'student',
  }));
  const guests = SAMPLE_GUEST_NAMES.map((name, index) => ({
    id: `sample-guest-${index}`,
    name,
    role: 'guest',
  }));
  const staff = SAMPLE_STAFF_NAMES.map((name, index) => ({
    id: `sample-staff-${index}`,
    name,
    role: 'staff',
  }));
  return [...students, ...guests, ...staff];
};

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
