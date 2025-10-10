import React, { useMemo } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  Paper,
  Stack,
  Typography,
} from '../../components';

const safeParse = (value, fallback) => {
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

const getDisplayName = (person) => {
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

const formatPeopleList = (people) =>
  people && people.length > 0
    ? people.map(getDisplayName).join(', ')
    : 'None';

const formatMultilineText = (text) => (text ? text : '');

const boolFromConfig = (value) => value === true || value === 'true';

const getPeopleAtCase = (caseItem) =>
  caseItem?.people_at_case || caseItem?.peopleAtCase || [];

const getPeopleByRole = (caseItem, roleId) =>
  getPeopleAtCase(caseItem)
    .filter((entry) => Number(entry.role) === Number(roleId))
    .map((entry) => entry.person)
    .filter(Boolean);

const caseIsEmpty = (caseItem) => {
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

const formatCaseDateTime = (caseItem) => {
  const segments = [];
  if (caseItem.date) {
    segments.push(caseItem.date);
  }
  if (caseItem.time) {
    segments.push(caseItem.time);
  }
  return segments.join(', ');
};

const formatLocation = (location) =>
  location && location.trim().length > 0 ? location : 'unknown';

const formatPlea = (plea, config) => {
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

const getRuleTitle = (charge) => {
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

const getResolutionPlanText = (charge) => {
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

const formatAwaitingMessage = (template, person, ruleTitle) => {
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
    message = message.replace(new RegExp(`\\{${key}\\}`, 'g'), value);
  });

  const ordered = [destination, replacements.person, replacements.rule];
  ordered.forEach((value, index) => {
    message = message.replace(new RegExp(`\\{${index}\\}`, 'g'), value);
  });

  return message;
};

const CaseCard = ({ caseItem, config, messages, roleIds }) => {
  if (caseIsEmpty(caseItem)) {
    return null;
  }

  const charges = Array.isArray(caseItem.charges) ? caseItem.charges : [];
  const testifiers = getPeopleByRole(caseItem, roleIds.testifier);
  const writers = roleIds.writer !== undefined ? getPeopleByRole(caseItem, roleIds.writer) : [];
  const isOpen = caseItem.dateClosed === null || caseItem.dateClosed === undefined;
  const filterNoCharges = boolFromConfig(config.filter_no_charge_cases);

  const showFindings = !(filterNoCharges && isOpen && charges.length === 0);
  const defaultFindings = `the ${config.str_res_plan || 'resolution plan'} was not completed.`;
  const findingsText = showFindings
    ? formatMultilineText(caseItem.compositeFindings || caseItem.findings || defaultFindings)
    : messages.findingsNotShownNoCharges ||
      'The findings are not shown here because this case has no charges and is not closed.';

  const testifierContent = filterNoCharges && charges.length === 0
    ? `${testifiers.length} ${testifiers.length === 1 ? 'person' : 'people'} testified.`
    : testifiers.length > 0
      ? formatPeopleList(testifiers)
      : null;

  const writerContent = writers.length > 0 ? formatPeopleList(writers) : null;

  const resolutionLabel = config.str_res_plan_short || 'Resolution plan';

  const chargeItems = charges.map((charge) => {
    const personName = charge.person ? getDisplayName(charge.person) : '???';
    const ruleTitle = getRuleTitle(charge);
    const severitySuffix = charge.severity ? ` (${charge.severity})` : '';
    const pleaText = formatPlea(charge.plea, config);
    const resolutionPlan = getResolutionPlanText(charge);

    const chargeNotes = [];
    if (charge.referredToSm && !charge.smDecision) {
      chargeNotes.push(
        formatAwaitingMessage(
          messages.awaitingSmDecision,
          personName,
          ruleTitle
        )
      );
    }
    if (charge.minorReferralDestination) {
      chargeNotes.push(
        `${messages.chargeAgainst || 'Charge against'} ${personName} with ${
          ruleTitle || ''
        } referred to ${charge.minorReferralDestination}.`
      );
    }
    if (charge.smDecision) {
      chargeNotes.push(
        `School meeting decision on charging ${personName} with ${
          ruleTitle || ''
        }: ${charge.smDecision}.`
      );
    }

    return (
      <Box component="li" key={charge.id || `${personName}-${ruleTitle}`}
        sx={{ mb: chargeNotes.length ? 1 : 0 }}
      >
        <Typography component="span" variant="body2">
          {personName !== '???' ? <strong>{personName}</strong> : '???'}
          {ruleTitle && (
            <>
              {personName !== '???' ? ': ' : ''}
              {ruleTitle}
              {ruleTitle.trim().endsWith('.') ? '' : '.'}
            </>
          )}
          {severitySuffix}
          {pleaText && (
            <>
              {' '}
              <strong>{pleaText}</strong>
            </>
          )}
          {resolutionPlan && (
            <>
              {' '}
              <Typography
                component="span"
                variant="body2"
                sx={{ textDecoration: 'underline', fontWeight: 500 }}
              >
                {`${resolutionLabel}: ${resolutionPlan}`}
              </Typography>
            </>
          )}
        </Typography>
        {chargeNotes.map((note, idx) => (
          <Typography key={idx} variant="body2" sx={{ mt: 0.5 }}>
            {note}
          </Typography>
        ))}
      </Box>
    );
  });

  return (
    <Paper
      sx={{
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 2,
        p: 2.25,
      }}
    >
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'baseline',
          flexWrap: 'wrap',
          rowGap: 1,
        }}
      >
        <Typography variant="h6" sx={{ fontWeight: 600 }}>
          {`Case #${caseItem.caseNumber}${isOpen ? ' (OPEN)' : ''}`}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {formatCaseDateTime(caseItem) || '—'} | Location: {formatLocation(caseItem.location)}
        </Typography>
      </Box>
      <Stack spacing={1} sx={{ mt: 1 }}>
          {boolFromConfig(config.track_writer) && (
            <Typography variant="body1">
              <strong>Writers:</strong>{' '}
              {writerContent ? (
                writerContent
              ) : (
                <Box component="span" sx={{ fontStyle: 'italic' }}>
                  None
                </Box>
              )}
            </Typography>
          )}
          <Typography variant="body1">
            <strong>{messages.whoTestified || 'Who testified:'}</strong>{' '}
            {testifierContent ? (
              testifierContent
            ) : (
              <Box component="span" sx={{ fontStyle: 'italic' }}>
                None
              </Box>
            )}
          </Typography>
          <Typography variant="body1" sx={{ whiteSpace: 'pre-line' }}>
            {findingsText}
          </Typography>
          <Box>
            <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
              {messages.chargesTitle || 'Charges'}
            </Typography>
            {charges.length > 0 ? (
              <Box component="ol" sx={{ pl: 3 }}>
                {chargeItems}
              </Box>
            ) : (
              <Typography variant="body2">
                {messages.noCharges || 'No charges.'}
              </Typography>
            )}
          </Box>
      </Stack>
    </Paper>
  );
};

const ReadMinutesPage = () => {
  const config = useMemo(() => safeParse(window.initialData?.config, {}), []);
  const messages = useMemo(() => safeParse(window.initialData?.messages, {}), []);

  const committee = useMemo(() => safeParse(window.initialData?.committee, []), []);
  const chair = useMemo(() => safeParse(window.initialData?.chair, []), []);
  const notetaker = useMemo(() => safeParse(window.initialData?.notetaker, []), []);
  const subs = useMemo(() => safeParse(window.initialData?.sub, []), []);
  const runners = useMemo(() => safeParse(window.initialData?.runners, []), []);

  const meetingCases = useMemo(
    () => safeParse(window.initialData?.meetingCases, []),
    []
  );
  const additionalCases = useMemo(
    () => safeParse(window.initialData?.additionalCases, []),
    []
  );

  const roleIds = useMemo(
    () => ({
      testifier: Number(window.initialData?.ROLE_TESTIFIER),
      writer: Number(window.initialData?.ROLE_WRITER),
    }),
    []
  );

  const meetingDate = window.initialData?.meetingDate || '';
  const printMinutesUrl = window.initialData?.printMinutesUrl;
  const viewResolutionPlansUrl = window.initialData?.viewResolutionPlansUrl;
  const downloadResolutionPlansUrl = window.initialData?.downloadResolutionPlansUrl;
  const editMinutesUrl = window.initialData?.editMinutesUrl;

  const filteredCases = useMemo(
    () => meetingCases.filter((caseItem) => !caseIsEmpty(caseItem)),
    [meetingCases]
  );

  const noMeetingContent =
    filteredCases.length === 0 &&
    additionalCases.length === 0 &&
    [committee, chair, notetaker, subs, runners].every(
      (group) => !group || group.length === 0
    );

  const mergedMessages = {
    committeeMembers: 'Committee members',
    caseNotes: 'Case Notes',
    chargesTitle: 'Charges',
    whoTestified: 'Who testified:',
    findingsNotShownNoCharges:
      'The findings are not shown here because this case has no charges and is not closed.',
    noCharges: 'No charges.',
    awaitingSmDecision:
      '*** Awaiting School Meeting decision on charging {person} with {rule}.',
    chargeAgainst: 'Charge against',
    casesToBeContinued: 'Cases to be continued',
    chooseCaseToContinue: 'Choose a case to continue',
    ...messages,
  };

  return (
    <Stack spacing={0.75} sx={{ mb: 2 }}>
      <Card sx={{ borderRadius: 3, boxShadow: '0 4px 18px rgba(15, 30, 60, 0.08)' }}>
        <CardHeader
          title={
            <Typography variant="h4" component="h1" sx={{ mb: 0 }}>
              {(config.str_jc_name_short || 'JC')} minutes from {meetingDate}
            </Typography>
          }
        />
        <CardContent sx={{ pt: 1, pb: 1.5 }}>
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={2}
            alignItems={{ md: 'center' }}
            flexWrap="wrap"
          >
            {printMinutesUrl && (
              <Button component="a" href={printMinutesUrl} target="_blank" rel="noopener noreferrer" variant="contained">
                <Typography>
                  1. Print minutes
                </Typography>
              </Button>
            )}
            {viewResolutionPlansUrl && (
              <Button component="a" href={viewResolutionPlansUrl} target="_blank" rel="noopener noreferrer" variant="contained">
                <Typography>
                  2. Print {config.str_res_plans || 'resolution plans'}
                </Typography>
              </Button>
            )}
            {downloadResolutionPlansUrl && (
              <Button component="a" href={downloadResolutionPlansUrl} target="_blank" rel="noopener noreferrer" variant="outlined">
                <Typography>
                  Download {config.str_res_plans || 'resolution plans'}
                </Typography>
              </Button>
            )}
            {editMinutesUrl && (
              <Button component="a" href={editMinutesUrl} variant="outlined">
                <Typography>
                  Edit minutes
                </Typography>
              </Button>
            )}
          </Stack>
        </CardContent>
      </Card>

      <Card sx={{ borderRadius: 3 }}>
        <CardHeader
          title={<Typography variant="h5">Committee &amp; Roles</Typography>}
        />
        <CardContent sx={{ pt: 1, pb: 1.5 }}>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: {
                xs: 'repeat(1, minmax(0, 1fr))',
                sm: 'repeat(2, minmax(0, 1fr))',
                lg: 'repeat(3, minmax(0, 1fr))',
              },
              columnGap: 4,
              rowGap: 0.75,
            }}
          >
            {[
              { label: 'Chair', value: chair },
              { label: 'Notetaker', value: notetaker },
              {
                label: mergedMessages.committeeMembers || 'Committee members',
                value: committee,
              },
              { label: 'Subs', value: subs },
              { label: 'Runners', value: runners },
            ].map(({ label, value }) => (
              <Box key={label}>
                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                  {label}
                </Typography>
                {value && value.length > 0 ? (
                  <Typography variant="body1">{formatPeopleList(value)}</Typography>
                ) : (
                  <Typography variant="body1" sx={{ fontStyle: 'italic' }}>
                    None
                  </Typography>
                )}
              </Box>
            ))}
          </Box>
        </CardContent>
      </Card>

      {noMeetingContent && (
        <Typography variant="body1">
          No meeting has happened yet today.
        </Typography>
      )}

      {filteredCases.length > 0 && (
        <Box>
          <Typography variant="h5" sx={{ mb: 1.5 }}>
            {mergedMessages.caseNotes || 'Case Notes'}
          </Typography>
          <Stack spacing={1.25}>
            {filteredCases.map((caseItem) => (
              <CaseCard
                key={caseItem.id || caseItem.caseNumber}
                caseItem={caseItem}
                config={config}
                messages={mergedMessages}
                roleIds={roleIds}
              />
            ))}
          </Stack>
        </Box>
      )}

      {additionalCases.length > 0 && (
        <Card sx={{ borderRadius: 3 }}>
          <CardHeader
            title={<Typography variant="h5">{mergedMessages.casesToBeContinued || 'Cases to be continued'}</Typography>}
          />
          <CardContent sx={{ pt: 1, pb: 1.5 }}>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 1 }}>
              {mergedMessages.chooseCaseToContinue || 'Choose a case to continue'}
            </Typography>
            <Stack spacing={1} direction={{ xs: 'column', md: 'row' }} flexWrap='wrap' rowGap={1} columnGap={1.5}>
              {additionalCases.map((additionalCase) => {
                const meetingId = additionalCase?.meeting?.id;
                const meetingDateShort = additionalCase?.meeting?.date
                  ? additionalCase.meeting.date
                  : '';
                return (
                  <Paper
                    key={additionalCase.id || additionalCase.caseNumber}
                    sx={{
                      p: 2,
                      borderRadius: 2,
                      border: '1px solid',
                      borderColor: 'divider',
                      flex: '1 1 300px',
                    }}
                  >
                    <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                      #{additionalCase.caseNumber}
                      {additionalCase.dateClosed === null ? ' (OPEN)' : ''}
                    </Typography>
                    <Typography variant="body1" color="text.secondary">
                      Case discussed today, but kept open for further investigation. Latest notes available in meeting held on{' '}
                      {meetingId ? (
                        <a href={`/viewMeeting/${meetingId}`} target="_blank" rel="noopener noreferrer">
                          {meetingDateShort || 'previous meeting'}
                        </a>
                      ) : (
                        meetingDateShort || 'previous meeting'
                      )}
                      .
                    </Typography>
                  </Paper>
                );
              })}
            </Stack>
          </CardContent>
        </Card>
      )}
    </Stack>
  );
};

export default ReadMinutesPage;
