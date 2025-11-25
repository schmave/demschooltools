export const TABLE_TEMPLATE =
  '48px minmax(200px, 1.4fr) repeat(5, minmax(100px, 1fr))';
export const NAME_CELL_TEMPLATE = 'minmax(0, 1fr) 60px';

export const LIGHT_BORDER_COLOR = 'rgba(0, 0, 0, 0.25)';
export const HEAVY_BORDER_COLOR = '#000';

const HEAVY_BORDER = `2px solid ${HEAVY_BORDER_COLOR}`;
const HEAVY_BORDER_THICK = `3px solid ${HEAVY_BORDER_COLOR}`;
const HEAVY_BORDER_THIN = `1px solid ${HEAVY_BORDER_COLOR}`;
const LIGHT_BORDER = `1px solid ${LIGHT_BORDER_COLOR}`;
const LIGHT_BORDER_THICK = `2px solid ${LIGHT_BORDER_COLOR}`;

const schoolDaySlotBase = {
  px: 1,
  py: 0.75,
  fontSize: '0.7rem',
  display: 'flex',
  alignItems: 'center',
};

const nameStatusBase = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  textTransform: 'uppercase',
  borderColor: LIGHT_BORDER_COLOR,
};

export const styles = {
  paper: { p: 3 },
  weekLabel: { fontWeight: 600 },
  tableWrapper: {
    mt: 2,
    border: HEAVY_BORDER_THICK,
    borderRadius: 2,
    overflow: 'hidden',
  },
  headerRow: {
    display: 'grid',
    gridTemplateColumns: TABLE_TEMPLATE,
    backgroundColor: 'grey.100',
    borderBottom: HEAVY_BORDER_THICK,
  },
  headerCell: {
    px: 1.5,
    py: 1,
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
    borderRight: HEAVY_BORDER_THIN,
  },
  headerIndexCell: {
    alignItems: 'center',
    fontWeight: 700,
  },
  headerNameCell: {
    display: 'grid',
    gridTemplateColumns: NAME_CELL_TEMPLATE,
    px: 0,
    py: 0,
  },
  headerNameLabel: {
    px: 1.5,
    py: 1,
    display: 'flex',
    alignItems: 'center',
    fontWeight: 700,
  },
  headerNameStatusColumn: {
    borderLeft: LIGHT_BORDER,
    display: 'grid',
    gridTemplateRows: 'repeat(2, minmax(32px, 1fr))',
  },
  headerNameStatusTop: {
    ...nameStatusBase,
    fontSize: '0.65rem',
    borderBottom: LIGHT_BORDER,
  },
  headerNameStatusBottom: {
    ...nameStatusBase,
    fontSize: '0.65rem',
  },
  headerDayTitle: {
    fontWeight: 700,
    textTransform: 'uppercase',
  },
  sectionRow: {
    display: 'grid',
    gridTemplateColumns: TABLE_TEMPLATE,
    borderBottom: HEAVY_BORDER_THICK,
  },
  sectionLabel: {
    gridColumn: '2 / span 6',
    px: 2,
    py: 1,
    textTransform: 'uppercase',
  },
  personRow: {
    display: 'grid',
    gridTemplateColumns: TABLE_TEMPLATE,
    borderBottom: HEAVY_BORDER_THICK,
    '&:last-of-type': {
      borderBottom: HEAVY_BORDER_THICK,
    },
  },
  indexCell: {
    px: 1.5,
    py: 1,
    borderRight: HEAVY_BORDER_THIN,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 600,
  },
  nameCell: {
    display: 'grid',
    gridTemplateColumns: NAME_CELL_TEMPLATE,
    borderRight: HEAVY_BORDER_THIN,
    alignItems: 'stretch',
  },
  nameCellName: {
    px: 1.5,
    py: 1,
    display: 'flex',
    alignItems: 'center',
  },
  nameCellStatusColumn: {
    borderLeft: LIGHT_BORDER,
    display: 'grid',
    gridTemplateRows: 'repeat(2, minmax(32px, 1fr))',
  },
  nameCellStatusTop: {
    ...nameStatusBase,
    borderBottom: LIGHT_BORDER,
  },
  nameCellStatusBottom: {
    ...nameStatusBase,
  },
  dayCell: {
    borderRight: HEAVY_BORDER_THIN,
  },
  noSchoolCell: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontStyle: 'italic',
    textTransform: 'uppercase',
    minHeight: 72,
    px: 1,
  },
  schoolDayCell: {
    display: 'grid',
    gridTemplateColumns: '2fr 1fr',
    gridTemplateRows: 'repeat(2, minmax(32px, 1fr))',
    minHeight: 72,
  },
  schoolDayLeftTop: {
    ...schoolDaySlotBase,
    borderRight: LIGHT_BORDER,
    borderBottom: LIGHT_BORDER,
    justifyContent: 'flex-start',
  },
  schoolDayRightTop: {
    ...schoolDaySlotBase,
    borderBottom: LIGHT_BORDER,
    justifyContent: 'center',
    backgroundColor: 'grey.300',
  },
  schoolDayLeftBottom: {
    ...schoolDaySlotBase,
    borderRight: LIGHT_BORDER,
    justifyContent: 'flex-start',
  },
  schoolDayRightBottom: {
    ...schoolDaySlotBase,
    justifyContent: 'center',
  },
};

export const getHeaderDayCellSx = (isLast) => ({
  ...styles.headerCell,
  borderRight: isLast ? 'none' : HEAVY_BORDER_THIN,
});

export const getDayCellSx = (isLast) => ({
  ...styles.dayCell,
  borderRight: isLast ? 'none' : HEAVY_BORDER_THIN,
});

export const getNoSchoolCellSx = (isLast) => ({
  ...getDayCellSx(isLast),
  ...styles.noSchoolCell,
});

export const getSchoolDayCellSx = (isLast) => ({
  ...getDayCellSx(isLast),
  ...styles.schoolDayCell,
});

export const borders = {
  heavy: HEAVY_BORDER,
  heavyThick: HEAVY_BORDER_THICK,
  heavyThin: HEAVY_BORDER_THIN,
  lightThick: LIGHT_BORDER_THICK,
  light: LIGHT_BORDER,
};
