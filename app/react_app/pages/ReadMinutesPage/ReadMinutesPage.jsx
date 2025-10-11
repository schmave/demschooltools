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
import { caseIsEmpty, formatPeopleList, safeParse } from '../../utils';
import CaseCard from './CaseCard';

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
      <Card>
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

      <Card>
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
                  <Typography>{formatPeopleList(value)}</Typography>
                ) : (
                  <Typography sx={{ fontStyle: 'italic' }}>
                    None
                  </Typography>
                )}
              </Box>
            ))}
          </Box>
        </CardContent>
      </Card>

      {noMeetingContent && (
        <Typography>
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
        <Card>
          <CardHeader
            title={<Typography variant="h5">{mergedMessages.casesToBeContinued || 'Cases to be continued'}</Typography>}
          />
          <CardContent sx={{ pt: 1, pb: 1.5 }}>
            <Typography color="text.secondary" sx={{ mb: 1 }}>
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
                    sx={{ flex: '1 1 300px' }}
                  >
                    <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                      #{additionalCase.caseNumber}
                      {additionalCase.dateClosed === null ? ' (OPEN)' : ''}
                    </Typography>
                    <Typography color="text.secondary">
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
