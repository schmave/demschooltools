import React from 'react';
import {
  Box,
  Divider,
  Paper,
  Stack,
  Typography,
  FitText,
} from '../../components';
import {
  DAYS,
} from './SignInUtils';
import {
  styles,
  getHeaderDayCellSx,
  getNoSchoolCellSx,
  getSchoolDayCellSx,
} from './SignInSheetTableStyles';

const SignInSheetTable = ({
  weekLabel,
  weekDates,
  quote,
  rows,
  schoolDays = DAYS,
  showHeader = true,
  schoolName = 'The Circle School',
  sheetTitle = 'Sign-In / Sign-Out Sheet',
}) => (
  <Paper elevation={0} sx={styles.paper}>
    {showHeader && (
      <Stack spacing={1.5}>
        <Box sx={{ display: 'flex', flexDirection: 'row', alignItems: 'flex-start', gap: 2 }}>
          <Box sx={{ flex: '0 0 auto', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
            <Typography variant="h6" sx={{ fontWeight: 700 }}>
              Week Of
            </Typography>
            <Typography variant="body1">
              {weekLabel}
            </Typography>
          </Box>
          <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
            <Typography variant="body2" color="text.secondary">
              {schoolName}
            </Typography>
            <Typography variant="subtitle1" sx={{ fontWeight: 500 }}>
              {sheetTitle}
            </Typography>
            {quote && (
              <Typography variant="body1" sx={{ fontStyle: 'italic', fontWeight: 700, mt: 0.5 }}>
                {quote}
              </Typography>
            )}
          </Box>
        </Box>
        <Divider />
      </Stack>
    )}

    <Box sx={styles.tableWrapper}>
      <Box sx={styles.headerRow}>
        <Box sx={{ ...styles.headerCell, ...styles.headerIndexCell }}>
          #
        </Box>
        <Box sx={{ ...styles.headerCell, ...styles.headerNameCell }}>
          <Box sx={styles.headerNameLabel}>
            Name
          </Box>
        </Box>
        {weekDates.map((date, dayIndex) => (
          <Box
            key={`${date.format('ddd')}-${dayIndex}`}
            sx={getHeaderDayCellSx(dayIndex === weekDates.length - 1)}
          >
            <Typography variant="body2" sx={styles.headerDayTitle}>
              {date.format('dddd')}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Time
            </Typography>
          </Box>
        ))}
      </Box>

      {rows.map((row, index) => {
        const isSection = row.type === 'section';
        const backgroundColor = isSection
          ? 'grey.100'
          : index % 2 === 0
          ? 'background.paper'
          : 'grey.50';

        if (isSection) {
          return (
            <Box
              key={`${row.label}-${index}`}
              sx={{ ...styles.sectionRow, backgroundColor }}
            >
              <Box />
              <Box sx={styles.sectionLabel}>
                {row.label}
              </Box>
            </Box>
          );
        }

        return (
          <Box
            key={row.id}
            sx={{ ...styles.personRow, backgroundColor }}
          >
            <Box sx={styles.indexCell}>
              {row.displayIndex}
            </Box>
            <Box sx={styles.nameCell}>
              <Box sx={styles.nameCellName}>
                <FitText maxSize={24} maxRows={2}>
                  {row.name}
                </FitText>
              </Box>
              <Box sx={styles.nameCellStatusColumn}>
                <Box sx={styles.nameCellStatusTop}>
                  In
                </Box>
                <Box sx={styles.nameCellStatusBottom}>
                  Out
                </Box>
              </Box>
            </Box>
            {DAYS.map((day, dayIndex) => {
              const isSchoolDay = schoolDays.includes(day);

              if (!isSchoolDay) {
                return (
                  <Box
                    key={`${row.id}-${day}`}
                    sx={getNoSchoolCellSx(dayIndex === DAYS.length - 1)}
                  >
                    No School
                  </Box>
                );
              }

              return (
                <Box
                  key={`${row.id}-${day}`}
                  sx={getSchoolDayCellSx(dayIndex === DAYS.length - 1)}
                >
                  <Box sx={styles.schoolDayLeftTop} />
                  <Box sx={styles.schoolDayRightTop} />
                  <Box sx={styles.schoolDayLeftBottom} />
                  <Box sx={styles.schoolDayRightBottom} />
                </Box>
              );
            })}
          </Box>
        );
      })}
    </Box>
  </Paper>
);

export default SignInSheetTable;
