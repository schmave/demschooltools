import React from 'react';
import { Box, Card, CardContent, Stack, Typography } from '../../components';
import {
  boolFromConfig,
  caseIsEmpty,
  formatAwaitingMessage,
  formatCaseDateTime,
  formatLocation,
  formatMultilineText,
  formatPeopleList,
  formatPlea,
  getDisplayName,
  getPeopleByRole,
  getResolutionPlanText,
  getRuleTitle,
} from '../../utils';
import SectionCardHeader from '../EditMinutesPage/SectionCardHeader';

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
        <Typography>
          {personName !== '???' ? <strong>{personName}</strong> : '???'}
          {ruleTitle && (
            <>
              {personName !== '???' ? ': ' : ''}
              {ruleTitle}
              {ruleTitle.trim().endsWith('.') ? '' : '.'}{' '}
            </>
          )}
          {severitySuffix}
          {pleaText && (
            <>
              <strong>{pleaText}</strong>
            </>
          )}
          {resolutionPlan && (
            <>
              <Box component="span" sx={{ textDecoration: 'underline', fontWeight: 500 }}>
                {`${resolutionLabel}: ${resolutionPlan}`}
              </Box>
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

  const headerAction = (
    <Typography variant="body2" color="text.secondary">
      {formatCaseDateTime(caseItem) || '—'} | Location: {formatLocation(caseItem.location)}
    </Typography>
  );

  return (
    <Card>
      <SectionCardHeader
        title={`Case #${caseItem.caseNumber}${isOpen ? ' (OPEN)' : ''}`}
        action={headerAction}
      />
      <CardContent>
        <Stack spacing={1}>
          {boolFromConfig(config.track_writer) && (
            <Typography>
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
          <Typography>
            <strong>{messages.whoTestified || 'Who testified:'}</strong>{' '}
            {testifierContent ? (
              testifierContent
            ) : (
              <Box component="span" sx={{ fontStyle: 'italic' }}>
                None
              </Box>
            )}
          </Typography>
          <Typography sx={{ whiteSpace: 'pre-line' }}>
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
      </CardContent>
    </Card>
  );
};

export default CaseCard;
