import React, {
  useCallback,
  useContext,
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
  buildSampleRoster,
  buildWeekDates,
  formatGeneratedAt,
  formatWeekLabel,
  parseRosterFromInitialData,
  splitRosterByRole,
  sortPeopleByName,
  DAYS,
} from './SignInUtils';

const SignInSheetPage = () => {
  const { setSnackbar } = useContext(SnackbarContext);
  const [quote, setQuote] = useState('');
  const [weekStart, setWeekStart] = useState(() =>
    dayjs().startOf('week').add(1, 'day')
  );
  const [isPrinting, setIsPrinting] = useState(false);
  const [schoolDays, setSchoolDays] = useState(() => [...DAYS]);

  const roster = useMemo(() => {
    const parsed = parseRosterFromInitialData();
    return parsed.length ? parsed : buildSampleRoster();
  }, []);

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
          <TextField
            label="Quote"
            value={quote}
            onChange={(event) => setQuote(event.target.value)}
            placeholder="Add an inspirational quote"
            fullWidth
          />
        </Stack>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <SignInSheetTable
            weekLabel={weekLabel}
            weekDates={weekDates}
            quote={quote}
            rows={studentRows}
            schoolDays={schoolDays}
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
              />
            </Stack>
          )}
        </Box>
      </Stack>
    </PageWrapper>
  );
};

export default SignInSheetPage;
