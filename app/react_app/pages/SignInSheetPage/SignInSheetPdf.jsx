import React from 'react';
import {
  Document,
  Page,
  StyleSheet,
  View,
  Text,
} from '@react-pdf/renderer';
import {
  DAYS,
} from './SignInUtils';

const HEAVY_BORDER = 1.25;
const LIGHT_BORDER = 0.6;
const POINTS_PER_INCH = 72;
const TABLE_WIDTH = POINTS_PER_INCH * 7.5; // 7.5 inches total
const DAY_COLUMN_WIDTH = POINTS_PER_INCH * 1.05; // 1.05 inches per day column
const INDEX_WIDTH = POINTS_PER_INCH * 0.25; // 0.25 inch for #
const NAME_WIDTH = TABLE_WIDTH - (DAY_COLUMN_WIDTH * DAYS.length + INDEX_WIDTH);
const IN_OUT_WIDTH = POINTS_PER_INCH * 0.35; // 0.35 inch for IN/OUT in name cell
const DIVIDER_COLOR = '#c2c7cc';
const SHADED_CELL = '#dadada';

// Split name into max 2 lines, only splitting on spaces
const splitNameIntoLines = (name, maxLineLength = 22) => {
  if (!name || name.length <= maxLineLength) {
    return [name || ''];
  }

  const words = name.split(' ');
  if (words.length === 1) {
    return [name];
  }

  let line1 = '';
  let line2 = '';
  
  for (let i = 0; i < words.length; i++) {
    const word = words[i];
    const testLine1 = line1 ? `${line1} ${word}` : word;
    
    if (testLine1.length <= maxLineLength || !line1) {
      line1 = testLine1;
    } else {
      line2 = words.slice(i).join(' ');
      break;
    }
  }

  return [line1, line2].filter(Boolean);
};

const getPdfFontSizeForTableName = (name) => {
  if (!name) {
    return 12;
  }
  const lines = splitNameIntoLines(name);
  const maxLen = Math.max(...lines.map(line => line.length));
  
  if (maxLen <= 16) {
    return 12;
  }
  if (maxLen <= 22) {
    return 11;
  }
  if (maxLen <= 28) {
    return 10;
  }
  return 9;
};

const pdfStyles = StyleSheet.create({
  page: {
    paddingTop: 20,
    paddingHorizontal: 36,
    paddingBottom: 24,
    fontFamily: 'Helvetica',
    color: '#111',
  },
  header: {
    marginBottom: 6,
    flexDirection: 'row',
  },
  headerLeft: {
    width: INDEX_WIDTH + NAME_WIDTH,
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerWeekLabel: {
    fontSize: 20,
    fontWeight: 700,
    fontFamily: 'Helvetica-Bold',
  },
  headerWeekRange: {
    fontSize: 12,
    fontWeight: 400,
    marginTop: 1,
  },
  headerRight: {
    flex: 1,
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'flex-start',
  },
  headerSchoolName: {
    fontSize: 11,
    fontWeight: 400,
    marginBottom: 2,
  },
  headerTitle: {
    fontSize: 14,
    fontWeight: 400,
    marginBottom: 2,
  },
  headerQuote: {
    fontSize: 13,
    fontFamily: 'Helvetica-BoldOblique',
    color: '#000',
    textAlign: 'center',
    marginTop: 2,
  },
  tableWrapper: {
    borderWidth: HEAVY_BORDER,
    borderColor: '#000',
    borderStyle: 'solid',
    overflow: 'hidden',
    width: TABLE_WIDTH,
    marginTop: 4,
  },
  tableHeaderRow: {
    flexDirection: 'row',
    backgroundColor: '#f2f2f2',
    borderBottomWidth: HEAVY_BORDER,
    borderColor: '#000',
  },
  headerCellIndex: {
    width: INDEX_WIDTH,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 3,
    fontSize: 9,
    fontWeight: 700,
    borderRightWidth: LIGHT_BORDER,
    borderColor: DIVIDER_COLOR,
  },
  headerCellName: {
    width: NAME_WIDTH,
    flexDirection: 'row',
    borderRightWidth: HEAVY_BORDER,
    borderColor: '#000',
    justifyContent: 'flex-start',
    alignItems: 'center',
    paddingVertical: 3,
    paddingHorizontal: 8,
  },
  headerNameLabel: {
    fontSize: 8,
    fontWeight: 700,
  },
  headerDayCell: {
    width: DAY_COLUMN_WIDTH,
    paddingVertical: 3,
    paddingHorizontal: 6,
    justifyContent: 'center',
    alignItems: 'center',
    borderRightWidth: HEAVY_BORDER,
    borderColor: '#000',
  },
  headerDayTitle: {
    fontSize: 8,
    fontWeight: 700,
    textTransform: 'uppercase',
  },
  headerDaySub: {
    fontSize: 6,
    color: '#666',
    marginTop: 1,
  },
  tableRow: {
    flexDirection: 'row',
    borderBottomWidth: HEAVY_BORDER,
    borderColor: '#000',
    minHeight: 34,
  },
  tableRowAlt: {
    backgroundColor: '#fff',
  },
  tableRowLast: {
    borderBottomWidth: 0,
  },
  indexCell: {
    width: INDEX_WIDTH,
    justifyContent: 'center',
    alignItems: 'center',
    fontSize: 7,
    fontWeight: 600,
    borderRightWidth: LIGHT_BORDER,
    borderColor: DIVIDER_COLOR,
  },
  nameCell: {
    width: NAME_WIDTH,
    flexDirection: 'row',
    alignItems: 'stretch',
    borderRightWidth: HEAVY_BORDER,
    borderColor: '#000',
  },
  nameCellValue: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'flex-start',
    paddingHorizontal: 6,
  },
  nameCellText: {
    fontWeight: 400,
    lineHeight: 1.15,
    textAlign: 'left',
  },
  nameInOutColumn: {
    width: IN_OUT_WIDTH,
    borderLeftWidth: LIGHT_BORDER,
    borderColor: DIVIDER_COLOR,
    backgroundColor: '#fff',
    justifyContent: 'space-between',
  },
  nameInOutCell: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    borderBottomWidth: LIGHT_BORDER,
    borderColor: DIVIDER_COLOR,
    fontSize: 7,
    fontWeight: 600,
    textTransform: 'uppercase',
  },
  nameInOutCellBottom: {
    borderBottomWidth: 0,
  },
  dayCell: {
    width: DAY_COLUMN_WIDTH,
    borderRightWidth: HEAVY_BORDER,
    borderColor: '#000',
    minHeight: 34,
    flexDirection: 'row',
  },
  dayCellLast: {
    borderRightWidth: 0,
  },
  noSchoolCell: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    fontSize: 8,
    fontStyle: 'italic',
    textTransform: 'uppercase',
    backgroundColor: '#fff',
    textAlign: 'center',
  },
  dayCellTimeColumn: {
    flex: 2,
    borderRightWidth: LIGHT_BORDER,
    borderColor: DIVIDER_COLOR,
    backgroundColor: '#fff',
    justifyContent: 'space-between',
  },
  dayCellTimeTop: {
    flex: 1,
    borderBottomWidth: LIGHT_BORDER,
    borderColor: DIVIDER_COLOR,
  },
  dayCellTimeBottom: {
    flex: 1,
  },
  dayCellCheckColumn: {
    flex: 1,
    backgroundColor: SHADED_CELL,
    justifyContent: 'space-between',
  },
  dayCellCheckTop: {
    flex: 1,
    borderBottomWidth: LIGHT_BORDER,
    borderColor: DIVIDER_COLOR,
    backgroundColor: SHADED_CELL,
  },
  dayCellCheckBottom: {
    flex: 1,
    backgroundColor: '#fff',
  },
  footer: {
    position: 'absolute',
    bottom: 24,
    left: 36,
    right: 36,
    fontSize: 8,
    color: '#666',
    textAlign: 'right',
  },
});

const PdfTableHeader = ({ weekDates }) => (
  <View style={pdfStyles.tableHeaderRow} fixed>
    <View style={pdfStyles.headerCellIndex}>
      <Text>#</Text>
    </View>
    <View style={pdfStyles.headerCellName}>
      <Text style={pdfStyles.headerNameLabel}>Name</Text>
    </View>
    {weekDates.map((date, index) => (
      <View
        key={`${date.format('ddd')}-${index}`}
        style={[
          pdfStyles.headerDayCell,
          index === weekDates.length - 1 && { borderRightWidth: 0 },
        ]}
      >
        <Text style={pdfStyles.headerDayTitle}>
          {date.format('dddd').toUpperCase()}
        </Text>
        <Text style={pdfStyles.headerDaySub}>Time</Text>
      </View>
    ))}
  </View>
);

const PdfDayCell = ({ isLast, isSchoolDay }) => {
  if (!isSchoolDay) {
    return (
      <View style={[pdfStyles.dayCell, isLast && pdfStyles.dayCellLast]}>
        <View style={pdfStyles.noSchoolCell}>
          <Text>NO SCHOOL</Text>
        </View>
      </View>
    );
  }

  return (
    <View style={[pdfStyles.dayCell, isLast && pdfStyles.dayCellLast]}>
      <View style={pdfStyles.dayCellTimeColumn}>
        <View style={pdfStyles.dayCellTimeTop} />
        <View style={pdfStyles.dayCellTimeBottom} />
      </View>
      <View style={pdfStyles.dayCellCheckColumn}>
        <View style={pdfStyles.dayCellCheckTop} />
        <View style={pdfStyles.dayCellCheckBottom} />
      </View>
    </View>
  );
};

const PdfRow = ({ row, isLast, schoolDays, rowIndex }) => {
  const isAlternate = rowIndex % 2 === 1;
  const nameLines = splitNameIntoLines(row.name);
  const fontSize = getPdfFontSizeForTableName(row.name);
  
  return (
    <View
      style={[
        pdfStyles.tableRow,
        isAlternate && pdfStyles.tableRowAlt,
        isLast && pdfStyles.tableRowLast,
      ]}
    >
      <View style={pdfStyles.indexCell}>
        <Text>{row.displayIndex || ''}</Text>
      </View>
      <View style={pdfStyles.nameCell}>
        <View style={pdfStyles.nameCellValue}>
          {nameLines.map((line, idx) => (
            <Text
              key={idx}
              style={[
                pdfStyles.nameCellText,
                { fontSize },
              ]}
            >
              {line}
            </Text>
          ))}
        </View>
        <View style={pdfStyles.nameInOutColumn}>
          <View style={pdfStyles.nameInOutCell}>
            <Text>IN</Text>
          </View>
          <View style={[pdfStyles.nameInOutCell, pdfStyles.nameInOutCellBottom]}>
            <Text>OUT</Text>
          </View>
        </View>
      </View>
      {DAYS.map((day, index) => (
        <PdfDayCell
          key={`${row.id}-${day}`}
          isLast={index === DAYS.length - 1}
          isSchoolDay={schoolDays.includes(day)}
        />
      ))}
    </View>
  );
};

const PdfTable = ({ rows, weekDates, schoolDays }) => (
  <View style={pdfStyles.tableWrapper}>
    <PdfTableHeader weekDates={weekDates} />
    {rows.map((row, index) => (
      <PdfRow
        key={row.id ?? `${row.name}-${index}`}
        row={row}
        isLast={index === rows.length - 1}
        schoolDays={schoolDays}
        rowIndex={index}
      />
    ))}
  </View>
);

const ROWS_PER_PAGE = 18;

const createBlankRow = (key, displayIndex) => ({
  id: `blank-${key}`,
  name: '',
  displayIndex,
  role: 'blank',
});

const chunkRows = (rows) => {
  const pages = [];
  const total = rows.length;
  const totalPages = Math.max(1, Math.ceil(total / ROWS_PER_PAGE));

  for (let page = 0; page < totalPages; page += 1) {
    const start = page * ROWS_PER_PAGE;
    const slice = rows.slice(start, start + ROWS_PER_PAGE).map((row, offset) => ({
      ...row,
      displayIndex: row.displayIndex ?? start + offset + 1,
    }));

    let nextDisplayIndex = start + slice.length + 1;
    while (slice.length < ROWS_PER_PAGE) {
      slice.push(createBlankRow(`${page}-${slice.length}`, nextDisplayIndex));
      nextDisplayIndex += 1;
    }

    pages.push(slice);
  }

  return pages;
};

const paginateRows = (rows) => {
  const filtered = rows.filter((row) => row && row.type !== 'section');
  const normalized = filtered.map((row, index) => ({
    ...row,
    displayIndex: row.displayIndex ?? index + 1,
  }));
  return chunkRows(normalized);
};

const buildGuestStaffPages = (rows, startingIndex) => {
  const guests = rows.filter((row) => row.role === 'guest' || row.role === 'volunteer');
  const staff = rows.filter((row) => row.role === 'staff');

  const total = guests.length + staff.length;
  const remainder = total % ROWS_PER_PAGE;
  const blankCount = remainder === 0 ? 0 : ROWS_PER_PAGE - remainder;

  // Assign displayIndex to guests
  const numberedGuests = guests.map((row, idx) => ({
    ...row,
    displayIndex: startingIndex + idx,
  }));

  // Create blank rows with correct numbering
  const blankRows = Array.from({ length: blankCount }, (_, idx) =>
    createBlankRow(`guest-staff-separator-${idx}`, startingIndex + guests.length + idx)
  );

  // Assign displayIndex to staff
  const numberedStaff = staff.map((row, idx) => ({
    ...row,
    displayIndex: startingIndex + guests.length + blankCount + idx,
  }));

  const combined = [...numberedGuests, ...blankRows, ...numberedStaff];

  // Chunk without re-assigning displayIndex
  const pages = [];
  const totalPages = Math.max(1, Math.ceil(combined.length / ROWS_PER_PAGE));

  for (let page = 0; page < totalPages; page += 1) {
    const start = page * ROWS_PER_PAGE;
    const slice = combined.slice(start, start + ROWS_PER_PAGE);
    
    // Fill remaining rows if needed
    const filledSlice = [...slice];
    let nextDisplayIndex = slice[slice.length - 1]?.displayIndex + 1 || startingIndex + combined.length;
    while (filledSlice.length < ROWS_PER_PAGE) {
      filledSlice.push(createBlankRow(`${page}-${filledSlice.length}`, nextDisplayIndex));
      nextDisplayIndex += 1;
    }

    pages.push(filledSlice);
  }

  return pages;
};

const buildPageDescriptors = (students, guestStaff) => {
  const descriptors = [];
  const studentChunks = paginateRows(students);
  
  // Calculate the starting index for guest/staff based on student count
  const studentCount = students.filter((row) => row && row.type !== 'section').length;
  // Account for blank rows added to fill the last student page
  const totalStudentRows = studentChunks.reduce((sum, chunk) => sum + chunk.length, 0);
  const startingGuestStaffIndex = totalStudentRows + 1;
  
  const guestChunks = buildGuestStaffPages(guestStaff, startingGuestStaffIndex);

  studentChunks.forEach((chunk, index) => {
    descriptors.push({
      key: `students-${index}`,
      rows: chunk,
    });
  });

  guestChunks.forEach((chunk, index) => {
    descriptors.push({
      key: `guests-${index}`,
      rows: chunk,
    });
  });

  if (descriptors.length === 0) {
    descriptors.push({ key: 'empty', rows: [] });
  }

  return descriptors;
};

const SignInSheetPdf = ({
  weekLabel,
  weekDates,
  quote,
  generatedAt,
  students,
  guestStaff,
  schoolDays,
  schoolName = 'The Circle School',
  sheetTitle = 'Sign-In / Sign-Out Sheet',
}) => {
  const pages = buildPageDescriptors(students, guestStaff);

  return (
    <Document>
      {pages.map((page) => (
        <Page key={page.key} size="LETTER" style={pdfStyles.page}>
          <View style={pdfStyles.header} fixed>
            <View style={pdfStyles.headerLeft}>
              <Text style={pdfStyles.headerWeekLabel}>Week Of</Text>
              <Text style={pdfStyles.headerWeekRange}>{weekLabel}</Text>
            </View>
            <View style={pdfStyles.headerRight}>
              <Text style={pdfStyles.headerSchoolName}>{schoolName}</Text>
              <Text style={pdfStyles.headerTitle}>{sheetTitle}</Text>
              {quote && (
                <Text style={pdfStyles.headerQuote}>{quote}</Text>
              )}
            </View>
          </View>

          <PdfTable rows={page.rows} weekDates={weekDates} schoolDays={schoolDays} />

          <Text style={pdfStyles.footer}>Generated {generatedAt}</Text>
        </Page>
      ))}
    </Document>
  );
};

export default SignInSheetPdf;
