import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import dayjs from 'dayjs';
import { pdf } from '@react-pdf/renderer';
import {
  Button,
  DatePicker,
  PageTitle,
  PageWrapper,
  Stack,
  TextField,
  SelectInput,
  Typography,
  Box,
} from '../../components';
import { SnackbarContext } from '../../contexts';
import SignInSheetTable from './SignInSheetTable';
import SignInSheetPdf from './SignInSheetPdf';
import {
  assignDisplayIndices,
  buildWeekDates,
  formatGeneratedAt,
  formatWeekLabel,
  splitRosterByRole,
  sortPeopleByName,
  DAYS,
} from './SignInUtils';
import { safeParse, normalizeOption, buildOptionMap } from '../../utils';

const SignInSheetPage = () => {
  const { setSnackbar } = useContext(SnackbarContext);
  
  // All useState declarations grouped together
  const [quote, setQuote] = useState('');
  const [sheetTitle, setSheetTitle] = useState('Sign-In / Sign-Out Sheet');
  const [weekStart, setWeekStart] = useState(() =>
    dayjs().startOf('week').add(1, 'day')
  );
  const [isPrinting, setIsPrinting] = useState(false);
  const [schoolDays, setSchoolDays] = useState(() => [...DAYS]);
  const [selectedStudentTags, setSelectedStudentTags] = useState([]);
  const [selectedStaffTags, setSelectedStaffTags] = useState([]);
  const [selectedGuests, setSelectedGuests] = useState([]);

  // Parse initial data
  const initialPeople = useMemo(
    () => safeParse(window.initialData?.people, []),
    []
  );
  const allPeople = useMemo(
    () => safeParse(window.initialData?.allPeople, []),
    []
  );
  const initialTags = useMemo(
    () => safeParse(window.initialData?.tags, []),
    []
  );

  // Build options and maps
  const peopleOptions = useMemo(
    () =>
      initialPeople
        .map((person) => normalizeOption(person))
        .filter(Boolean),
    [initialPeople]
  );
  const peopleOptionMap = useMemo(
    () => buildOptionMap(peopleOptions),
    [peopleOptions]
  );

  const allPeopleOptions = useMemo(
    () =>
      allPeople
        .map((person) => normalizeOption(person))
        .filter(Boolean),
    [allPeople]
  );
  const allPeopleOptionMap = useMemo(
    () => buildOptionMap(allPeopleOptions),
    [allPeopleOptions]
  );

  const tagOptions = useMemo(
    () =>
      initialTags
        .map((tag) => normalizeOption(tag))
        .filter(Boolean),
    [initialTags]
  );
  const tagOptionMap = useMemo(
    () => buildOptionMap(tagOptions),
    [tagOptions]
  );

  // Initialize default tag selections on mount
  useEffect(() => {
    const currentStudentTag = tagOptions.find(
      (tag) => tag.label === 'Current Student'
    );
    const staffTag = tagOptions.find((tag) => tag.label === 'Staff');

    if (currentStudentTag) {
      setSelectedStudentTags([currentStudentTag.id]);
    }
    if (staffTag) {
      setSelectedStaffTags([staffTag.id]);
    }
  }, [tagOptions]);

  // Build roster from selected tags and people
  const roster = useMemo(() => {
    const result = [];

    // Add students based on selected tags
    if (selectedStudentTags.length > 0) {
      const studentTagSet = new Set(selectedStudentTags.map(String));
      initialPeople.forEach((person) => {
        const personTags = person.tags || [];
        const hasStudentTag = personTags.some((tagId) =>
          studentTagSet.has(String(tagId))
        );
        if (hasStudentTag) {
          result.push({
            id: person.id,
            name: person.label || person.name || '',
            role: 'student',
          });
        }
      });
    }

    // Add staff based on selected tags
    if (selectedStaffTags.length > 0) {
      const staffTagSet = new Set(selectedStaffTags.map(String));
      initialPeople.forEach((person) => {
        const personTags = person.tags || [];
        const hasStaffTag = personTags.some((tagId) =>
          staffTagSet.has(String(tagId))
        );
        if (hasStaffTag) {
          // Check if already added as student
          if (!result.some((p) => p.id === person.id)) {
            result.push({
              id: person.id,
              name: person.label || person.name || '',
              role: 'staff',
            });
          }
        }
      });
    }

    // Add guests based on selected people
    selectedGuests.forEach((guestId) => {
      const person = allPeopleOptionMap.get(String(guestId));
      if (person && !result.some((p) => p.id === person.id)) {
        result.push({
          id: person.id,
          name: person.label,
          role: 'guest',
        });
      }
    });

    return result;
  }, [
    selectedStudentTags,
    selectedStaffTags,
    selectedGuests,
    initialPeople,
    allPeopleOptionMap,
  ]);

  const { students, guests, staff } = useMemo(
    () => splitRosterByRole(roster),
    [roster]
  );

  const studentsSorted = useMemo(
    () => [...students].sort(sortPeopleByName),
    [students]
  );
  const guestsSorted = useMemo(
    () => [...guests].sort(sortPeopleByName),
    [guests]
  );
  const staffSorted = useMemo(
    () => [...staff].sort(sortPeopleByName),
    [staff]
  );

  const {
    students: studentsWithIndex,
    guests: guestsWithIndex,
    staff: staffWithIndex,
  } = useMemo(
    () =>
      assignDisplayIndices(studentsSorted, guestsSorted, staffSorted),
    [studentsSorted, guestsSorted, staffSorted]
  );

  const studentRows = studentsWithIndex;
  const guestStaffRows = useMemo(
    () => [...guestsWithIndex, ...staffWithIndex],
    [guestsWithIndex, staffWithIndex]
  );

  const weekDates = useMemo(
    () => buildWeekDates(weekStart),
    [weekStart]
  );

  const weekLabel = useMemo(
    () => formatWeekLabel(weekDates),
    [weekDates]
  );

  const schoolDayOptions = useMemo(
    () => DAYS.map((day) => ({ value: day, label: day })),
    []
  );

  const handleError = useCallback(
    (message, error) => {
      // eslint-disable-next-line no-console
      console.error(message, error);
      setSnackbar({ message, severity: 'error' });
    },
    [setSnackbar]
  );

  const handleWeekSelection = useCallback(
    (newValue) => {
      if (!newValue) {
        return;
      }
      const monday = newValue.startOf('week').add(1, 'day');
      setWeekStart(monday);
    },
    [setWeekStart]
  );

  const handlePrint = useCallback(async () => {
    setIsPrinting(true);
    try {
      const generatedAt = formatGeneratedAt();
      const doc = (
        <SignInSheetPdf
          weekLabel={weekLabel}
          weekDates={weekDates}
          quote={quote}
          generatedAt={generatedAt}
          students={studentRows}
          guestStaff={guestStaffRows}
          schoolDays={schoolDays}
          sheetTitle={sheetTitle}
        />
      );
      const blob = await pdf(doc).toBlob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `SignInSheet_${weekDates[0].format('YYYY-MM-DD')}.pdf`;
      link.click();
      setSnackbar({
        message: 'Sign-in sheet PDF downloaded',
        severity: 'success',
      });
      setTimeout(() => URL.revokeObjectURL(url), 100);
    } catch (error) {
      handleError('Unable to generate sign-in sheet PDF', error);
    } finally {
      setIsPrinting(false);
    }
  }, [
    guestStaffRows,
    handleError,
    quote,
    schoolDays,
    setSnackbar,
    studentRows,
    weekDates,
    weekLabel,
    sheetTitle,
  ]);

  return (
    <PageWrapper surface>
      <Stack spacing={3}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          alignItems={{ xs: 'flex-start', sm: 'center' }}
          justifyContent="space-between"
          spacing={2}
        >
          <PageTitle>Print Sign In Sheet</PageTitle>
          <Button
            variant="contained"
            onClick={handlePrint}
            disabled={isPrinting}
          >
            {isPrinting ? 'Preparing PDF…' : 'Print Sign In Sheet'}
          </Button>
        </Stack>

        <Stack spacing={2}>
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
            alignItems={{ md: 'center' }}
          >
            <DatePicker
              label="Week of"
              value={weekStart}
              setValue={handleWeekSelection}
            />
            <SelectInput
              size="medium"
              showClearButton
              label="School Days"
              multiple
              value={schoolDays}
              setValue={setSchoolDays}
              options={schoolDayOptions}
              placeholder="Select days"
              fullWidth
            />
          </Stack>

          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
          >
            <TextField
              label="Sheet Title"
              value={sheetTitle}
              onChange={(event) => setSheetTitle(event.target.value)}
              placeholder="Sign-In / Sign-Out Sheet"
              fullWidth
            />
            <TextField
              label="Quote"
              value={quote}
              onChange={(event) => setQuote(event.target.value)}
              placeholder="Add an inspirational quote"
              fullWidth
            />
          </Stack>

          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
          >
            <SelectInput
              autocomplete
              multiple
              label="Students"
              options={tagOptions.map((tag) => ({ ...tag, value: tag.id, label: tag.label }))}
              value={selectedStudentTags}
              onChange={(e, newIds) => {
                setSelectedStudentTags(Array.isArray(newIds) ? newIds : []);
              }}
              placeholder="Select student tags"
              size="medium"
              fullWidth
              showClearButton
            />
            <SelectInput
              autocomplete
              multiple
              label="Staff"
              options={tagOptions.map((tag) => ({ ...tag, value: tag.id, label: tag.label }))}
              value={selectedStaffTags}
              onChange={(e, newIds) => {
                setSelectedStaffTags(Array.isArray(newIds) ? newIds : []);
              }}
              placeholder="Select staff tags"
              size="medium"
              fullWidth
              showClearButton
            />
            <SelectInput
              autocomplete
              multiple
              label="Guests"
              options={allPeopleOptions.map((person) => ({ ...person, value: person.id, label: person.label }))}
              value={selectedGuests}
              onChange={(e, newIds) => {
                setSelectedGuests(Array.isArray(newIds) ? newIds : []);
              }}
              placeholder="Select guests"
              size="medium"
              fullWidth
              showClearButton
            />
          </Stack>
        </Stack>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <SignInSheetTable
            weekLabel={weekLabel}
            weekDates={weekDates}
            quote={quote}
            rows={studentRows}
            schoolDays={schoolDays}
            sheetTitle={sheetTitle}
          />
          {guestStaffRows.length > 0 && (
            <Stack spacing={1.5}>
              <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                Guests & Staff
              </Typography>
              <SignInSheetTable
                weekLabel={weekLabel}
                weekDates={weekDates}
                rows={guestStaffRows}
                schoolDays={schoolDays}
                showHeader={false}
                sheetTitle={sheetTitle}
              />
            </Stack>
          )}
        </Box>
      </Stack>
    </PageWrapper>
  );
};

export default SignInSheetPage;
