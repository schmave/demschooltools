import React, { useCallback, useEffect, useMemo } from 'react';
import {
  Box,
  Button,
  Checkbox,
  FormControl,
  FormControlLabel,
  FormLabel,
  Paper,
  Radio,
  RadioGroup,
  Stack,
  TextField,
  Typography,
  SelectInput,
} from '../../components';
import { TIME_SERVED_LABEL } from './constants';

const ChargeCard = ({
  caseId,
  charge,
  config,
  messages,
  peopleOptions,
  peopleOptionMap,
  ruleOptions,
  onUpdateCharge,
  onRemoveCharge,
  onShowPersonRuleHistory,
  fetchLastResolutionPlan,
}) => {
  const handlePersonChange = useCallback(
    (people) => {
      const person = people[0] || null;
      onUpdateCharge(
        caseId,
        charge.id,
        (prev) => ({
          ...prev,
          person,
        }),
        { queueSave: true },
      );

      if (person?.id && charge.rule?.id) {
        fetchLastResolutionPlan(person.id, charge.rule.id).then((html) => {
          onUpdateCharge(caseId, charge.id, (prev) => ({ ...prev, lastResolutionHtml: html }));
        });
      } else {
        onUpdateCharge(caseId, charge.id, (prev) => ({ ...prev, lastResolutionHtml: '' }));
      }
    },
    [caseId, charge.id, charge.rule?.id, fetchLastResolutionPlan, onUpdateCharge],
  );

  const handleRuleChange = useCallback(
    (event, newId) => {
      const normalizedId = newId ? String(newId) : '';
      const matchingOption =
        ruleOptions.find((option) => String(option.id) === normalizedId) || null;
      const option = matchingOption ? { id: matchingOption.id, label: matchingOption.label } : null;

      onUpdateCharge(
        caseId,
        charge.id,
        (prev) => ({
          ...prev,
          rule: option,
        }),
        { queueSave: true },
      );

      if (charge.person?.id && option?.id) {
        fetchLastResolutionPlan(charge.person.id, option.id).then((html) => {
          onUpdateCharge(caseId, charge.id, (prev) => ({ ...prev, lastResolutionHtml: html }));
        });
      } else {
        onUpdateCharge(caseId, charge.id, (prev) => ({ ...prev, lastResolutionHtml: '' }));
      }
    },
    [caseId, charge.id, charge.person?.id, fetchLastResolutionPlan, onUpdateCharge, ruleOptions],
  );

  const handlePleaChange = useCallback(
    (event) => {
      const value = event.target.value;
      onUpdateCharge(
        caseId,
        charge.id,
        (prev) => ({
          ...prev,
          plea: value,
          referredToSm:
            value === 'Not Guilty'
              ? true
              : prev.referredToSm,
        }),
        { queueSave: true },
      );
    },
    [caseId, charge.id, onUpdateCharge],
  );

  const handleSeverityChange = useCallback(
    (event) => {
      const value = event.target.value;
      onUpdateCharge(
        caseId,
        charge.id,
        (prev) => ({ ...prev, severity: value }),
        { queueSave: true },
      );
    },
    [caseId, charge.id, onUpdateCharge],
  );

  const handleReferredChange = useCallback(
    (event) => {
      const checked = event.target.checked;
      onUpdateCharge(
        caseId,
        charge.id,
        (prev) => ({ ...prev, referredToSm: checked }),
        { queueSave: true },
      );
    },
    [caseId, charge.id, onUpdateCharge],
  );

  const handleMinorReferralChange = useCallback(
    (event) => {
      const value = event.target.value;
      onUpdateCharge(
        caseId,
        charge.id,
        (prev) => ({ ...prev, minorReferralDestination: value }),
        { queueSave: true },
      );
    },
    [caseId, charge.id, onUpdateCharge],
  );

  const handleResolutionChange = useCallback(
    (event) => {
      const value = event.target.value;
      onUpdateCharge(
        caseId,
        charge.id,
        (prev) => ({ ...prev, resolutionPlan: value }),
        { queueSave: true },
      );
    },
    [caseId, charge.id, onUpdateCharge],
  );

  const handleFollowUpChange = useCallback(
    (event) => {
      const value = event.target.value;
      let nextChoice = 'original';
      let nextPlan = charge.resolutionPlan;

      if (value === 'timeServed') {
        nextChoice = 'timeServed';
        nextPlan = TIME_SERVED_LABEL;
      } else if (value === 'newPlan') {
        nextChoice = 'newPlan';
      }

      if (nextChoice === 'original') {
        nextPlan = '';
      }

      onUpdateCharge(
        caseId,
        charge.id,
        (prev) => ({
          ...prev,
          followUpChoice: nextChoice,
          resolutionPlan: nextPlan,
        }),
        { queueSave: true },
      );
    },
    [caseId, charge.id, charge.resolutionPlan, onUpdateCharge],
  );

  useEffect(() => {
    if (charge.person?.id && charge.rule?.id && !charge.lastResolutionHtml) {
      fetchLastResolutionPlan(charge.person.id, charge.rule.id).then((html) => {
        onUpdateCharge(caseId, charge.id, (prev) => ({ ...prev, lastResolutionHtml: html }));
      });
    }
  }, [caseId, charge.id, charge.person?.id, charge.rule?.id, charge.lastResolutionHtml, fetchLastResolutionPlan, onUpdateCharge]);

  const pleaOptions = useMemo(() => {
    const options = [
      { value: 'Guilty', label: config.str_guilty || 'Guilty' },
    ];
    if (config.show_no_contest_plea) {
      options.push({ value: 'No Contest', label: 'No Contest' });
    }
    options.push({ value: 'Not Guilty', label: config.str_not_guilty || 'Not Guilty' });
    if (config.show_na_plea) {
      options.push({ value: 'N/A', label: config.str_na || 'N/A' });
    }
    return options;
  }, [config.str_guilty, config.show_no_contest_plea, config.str_not_guilty, config.show_na_plea, config.str_na]);

  const severityOptions = useMemo(() => ['Mild', 'Moderate', 'Serious', 'Severe'], []);

  const followUpValue =
    charge.followUpChoice === 'timeServed'
      ? 'timeServed'
      : charge.followUpChoice === 'newPlan'
      ? 'newPlan'
      : '';

  const showResolutionInput =
    !charge.referencedSource || charge.followUpChoice === 'newPlan';

  const ruleSelectOptions = useMemo(
    () =>
      ruleOptions.map((option) => ({
        ...option,
        value: String(option.id),
        label: option.label || option.title || '',
      })),
    [ruleOptions],
  );

  return (
    <Paper
      sx={{
        p: 2,
        borderLeft: charge.referencedSource ? '4px solid #1976d2' : '1px solid',
        borderColor: charge.referencedSource ? 'primary.main' : 'divider',
        borderRadius: 1,
      }}
    >
      <Stack spacing={2}>
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          spacing={2}
          alignItems={{ md: 'center' }}
        >
          <Box sx={{ flex: 1 }}>
            <SelectInput
              autocomplete
              label={messages.chargeAgainst || 'Charge against'}
              options={peopleOptions.map((p) => ({ ...p, value: p.id, label: p.label }))}
              value={charge.person?.id || ''}
              onChange={(e, newId) => {
                const person =
                  peopleOptionMap?.get(String(newId)) ||
                  peopleOptions.find((p) => p.id === String(newId)) ||
                  null;
                handlePersonChange(person ? [person] : []);
              }}
              size="small"
              fullWidth
            />
          </Box>
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={1.5}
            alignItems={{ sm: 'center' }}
            sx={{ flexShrink: 0 }}
          >
            <FormControlLabel
              control={
                <Checkbox
                  checked={charge.referredToSm}
                  onChange={handleReferredChange}
                />
              }
              label="Refer to School Meeting"
              sx={{ m: 0 }}
            />
            {config.use_minor_referrals && (
              <TextField
                label="Refer to"
                value={charge.minorReferralDestination || ''}
                onChange={handleMinorReferralChange}
                size="small"
              />
            )}
          </Stack>
        </Stack>

        {config.show_entry && (
          <SelectInput
            autocomplete
            label="Rule"
            options={ruleSelectOptions}
            value={charge.rule?.id ? String(charge.rule.id) : ''}
            onChange={handleRuleChange}
            placeholder="Search rules"
            size="small"
          />
        )}

        {config.show_plea && (
          <FormControl>
            <FormLabel>Plea</FormLabel>
            <RadioGroup
              row
              value={charge.plea || ''}
              onChange={handlePleaChange}
            >
              {pleaOptions.map((option) => (
                <FormControlLabel
                  key={option.value}
                  value={option.value}
                  control={<Radio />}
                  label={option.label}
                />
              ))}
            </RadioGroup>
          </FormControl>
        )}

        {config.show_severity && (
          <FormControl>
            <FormLabel>Severity</FormLabel>
            <RadioGroup
              row
              value={charge.severity || ''}
              onChange={handleSeverityChange}
            >
              {severityOptions.map((option) => (
                <FormControlLabel
                  key={option}
                  value={option}
                  control={<Radio />}
                  label={option}
                />
              ))}
            </RadioGroup>
          </FormControl>
        )}

        {charge.referencedSource && (
          <Box>
            <FormLabel>Follow-up</FormLabel>
            <RadioGroup
              value={followUpValue}
              onChange={handleFollowUpChange}
            >
              <FormControlLabel
                value="timeServed"
                control={<Radio />}
                label="Time served"
              />
              <FormControlLabel
                value="newPlan"
                control={<Radio />}
                label={`Delete & replace original ${
                  config.str_res_plan || 'resolution plan'
                }`}
              />
            </RadioGroup>
            <Typography variant="body2" color="text.secondary">
              Original: {charge.referencedSource.resolutionPlan}
            </Typography>
          </Box>
        )}

        {showResolutionInput && (
          <TextField
            label={config.str_res_plan_cap || 'Resolution plan'}
            multiline
            minRows={2}
            value={charge.resolutionPlan}
            onChange={handleResolutionChange}
          />
        )}

        {charge.lastResolutionHtml ? (
          <Box
            sx={{ fontSize: 13, color: 'text.secondary' }}
            onClick={(event) => {
              const moreInfo = event.target.closest('.more-info');
              if (moreInfo && charge.person?.id && charge.rule?.id) {
                event.preventDefault();
                onShowPersonRuleHistory(charge.person.id, charge.rule.id);
              }
            }}
            dangerouslySetInnerHTML={{ __html: charge.lastResolutionHtml }}
          />
        ) : null}

        <Stack direction="row" spacing={1} alignItems="center">
          <Button
            color="error"
            disabled={charge.isReferenced}
            onClick={() => onRemoveCharge(caseId, charge.id)}
            size="small"
          >
            {messages.removeCharge || 'Remove charge'}
          </Button>
          {charge.isReferenced && (
            <Typography variant="caption" color="text.secondary">
              {messages.noDeleteReferencedCharge ||
                'This charge cannot be deleted because it is referenced elsewhere.'}
            </Typography>
          )}
        </Stack>
      </Stack>
    </Paper>
  );
};

export default ChargeCard;
