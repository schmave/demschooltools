import React, { useCallback, useEffect, useMemo } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  Checkbox,
  FormControlLabel,
  SelectInput,
  Stack,
  TextField,
} from '../../components';
import ChargeCard from './ChargeCard';
import CaseReferences from './CaseReferences';

const CaseCard = ({
  caseItem,
  config,
  messages,
  peopleOptions,
  peopleOptionMap,
  ruleOptions,
  caseOptions,
  roleIds,
  enableCaseReferences,
  onUpdateCase,
  onAddCharge,
  onUpdateCharge,
  onRemoveCharge,
  onAddPersonAtCase,
  onRemovePersonAtCase,
  onAddReferencedCase,
  onRemoveReferencedCase,
  onToggleReferencedCharge,
  onGenerateChargeFromReference,
  onRequestClearCase,
  onRefreshReferences,
  onShowPersonRuleHistory,
  fetchLastResolutionPlan,
}) => {
  useEffect(() => {
    if (enableCaseReferences && !caseItem.referencesLoaded) {
      onRefreshReferences(caseItem.id);
    }
  }, [caseItem.id, caseItem.referencesLoaded, enableCaseReferences, onRefreshReferences]);
  const handleFieldChange = useCallback(
    (field) => (event) => {
      const value = event.target.value;
      onUpdateCase(
        caseItem.id,
        (prev) => ({
          ...prev,
          [field]: value,
        }),
        { queueSave: true },
      );
    },
    [caseItem.id, onUpdateCase],
  );

  const handleContinuedChange = useCallback(
    (event) => {
      const checked = event.target.checked;
      onUpdateCase(
        caseItem.id,
        (prev) => ({
          ...prev,
          continued: checked,
        }),
        { queueSave: true },
      );
    },
    [caseItem.id, onUpdateCase],
  );

  const handleTestifierChange = useCallback(
    (people) => {
      onUpdateCase(caseItem.id, (prev) => ({ ...prev, testifiers: people }));
    },
    [caseItem.id, onUpdateCase],
  );

  const handleWriterChange = useCallback(
    (people) => {
      onUpdateCase(caseItem.id, (prev) => ({ ...prev, writers: people }));
    },
    [caseItem.id, onUpdateCase],
  );

  const handleReferencedCasesChange = useCallback(
    (event, newIds) => {
      const nextIds = new Set((Array.isArray(newIds) ? newIds : []).map(String));
      const currentReferences = caseItem.caseReferences || [];
      const currentIds = new Set(currentReferences.map((reference) => String(reference.id)));

      nextIds.forEach((id) => {
        const numericId = Number(id);
        if (!currentIds.has(id) && numericId !== caseItem.id) {
          onAddReferencedCase(caseItem.id, numericId);
        }
      });

      currentReferences.forEach((reference) => {
        const referenceId = String(reference.id);
        if (!nextIds.has(referenceId)) {
          onRemoveReferencedCase(caseItem.id, Number(reference.id));
        }
      });
    },
    [caseItem.caseReferences, caseItem.id, onAddReferencedCase, onRemoveReferencedCase],
  );

  const testifierRole = Number(roleIds.testifier);
  const writerRole = Number(roleIds.writer);

  const personSelectOptions = useMemo(
    () => peopleOptions.map((p) => ({ ...p, value: p.id, label: p.label })),
    [peopleOptions],
  );

  const referencedCaseOptions = useMemo(
    () =>
      caseOptions
        .filter((option) => Number(option.id) !== caseItem.id)
        .map((option) => ({
          ...option,
          value: String(option.id),
          label: option.label || option.caseNumber || '',
        })),
    [caseOptions, caseItem.id],
  );

  const resolvePeopleByIds = useCallback(
    (ids) =>
      (Array.isArray(ids) ? ids : [])
        .map((id) => peopleOptionMap.get(String(id)) || peopleOptions.find((p) => p.id === id || p.id === String(id)))
        .filter(Boolean),
    [peopleOptionMap, peopleOptions],
  );

  return (
    <Card>
      <CardHeader
        title={`Case #${caseItem.caseNumber}`}
        action={
          <Button color="error" onClick={() => onRequestClearCase(caseItem.id)}>
            {messages.eraseCase || 'Erase case'}
          </Button>
        }
      />
      <CardContent>
        <Stack spacing={3}>
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
            alignItems={{ md: 'flex-end' }}
          >
            <TextField
              label="Location"
              value={caseItem.location}
              onChange={handleFieldChange('location')}
              fullWidth
            />
            <TextField
              label="Date of event"
              type="date"
              value={caseItem.date || ''}
              onChange={handleFieldChange('date')}
              InputLabelProps={{ shrink: true }}
              sx={{ minWidth: 180 }}
            />
            <TextField
              label="Time"
              value={caseItem.time}
              onChange={handleFieldChange('time')}
              sx={{ minWidth: 140 }}
            />
          </Stack>

          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
            alignItems={{ md: 'flex-start' }}
          >
            <Box sx={{ flex: '1 1 0', minWidth: { xs: '100%', md: 220 } }}>
              <SelectInput
                autocomplete
                multiple
                label={messages.whoTestified || 'Who testified'}
                options={personSelectOptions}
                value={caseItem.testifiers.map((p) => p.id)}
                onChange={(e, newIds) => {
                  const newPeople = resolvePeopleByIds(newIds);
                  const prevIds = new Set(caseItem.testifiers.map((p) => p.id));
                  const nextIds = new Set(newPeople.map((p) => p.id));
                  newPeople.forEach((p) => {
                    if (!prevIds.has(p.id)) {
                      onAddPersonAtCase(caseItem.id, p, testifierRole);
                    }
                  });
                  caseItem.testifiers.forEach((p) => {
                    if (!nextIds.has(p.id)) {
                      onRemovePersonAtCase(caseItem.id, p, testifierRole);
                    }
                  });
                  handleTestifierChange(newPeople);
                }}
                placeholder="Search people"
                size="medium"
                fullWidth
              />
            </Box>
            {config.track_writer && (
              <Box sx={{ flex: '1 1 0', minWidth: { xs: '100%', md: 220 } }}>
                <SelectInput
                  autocomplete
                  multiple
                  label={messages.whoWroteComplaint || 'Who wrote complaint'}
                  options={personSelectOptions}
                  value={caseItem.writers.map((p) => p.id)}
                  onChange={(e, newIds) => {
                    const newPeople = resolvePeopleByIds(newIds);
                    const prevIds = new Set(caseItem.writers.map((p) => p.id));
                    const nextIds = new Set(newPeople.map((p) => p.id));
                    newPeople.forEach((p) => {
                      if (!prevIds.has(p.id)) {
                        onAddPersonAtCase(caseItem.id, p, writerRole);
                      }
                    });
                    caseItem.writers.forEach((p) => {
                      if (!nextIds.has(p.id)) {
                        onRemovePersonAtCase(caseItem.id, p, writerRole);
                      }
                    });
                    handleWriterChange(newPeople);
                  }}
                  placeholder="Search people"
                  size="medium"
                  fullWidth
                />
              </Box>
            )}
            {enableCaseReferences && (
              <Box sx={{ flex: '1 1 0', minWidth: { xs: '100%', md: 220 } }}>
                <SelectInput
                  autocomplete
                  multiple
                  label={messages.referencedCases || 'Referenced issues'}
                  placeholder="Search by case number"
                  options={referencedCaseOptions}
                  value={(caseItem.caseReferences || []).map((reference) => String(reference.id))}
                  onChange={handleReferencedCasesChange}
                  size="medium"
                />
              </Box>
            )}
          </Stack>

          <TextField
            label={config.str_findings || 'Findings'}
            multiline
            minRows={3}
            value={caseItem.findings}
            onChange={handleFieldChange('findings')}
          />

          <Stack spacing={2}>
            {caseItem.charges.map((charge) => (
              <ChargeCard
                key={charge.id}
                caseId={caseItem.id}
                charge={charge}
                config={config}
                messages={messages}
                peopleOptions={peopleOptions}
                peopleOptionMap={peopleOptionMap}
                ruleOptions={ruleOptions}
                onUpdateCharge={onUpdateCharge}
                onRemoveCharge={onRemoveCharge}
                onShowPersonRuleHistory={onShowPersonRuleHistory}
                fetchLastResolutionPlan={fetchLastResolutionPlan}
              />
            ))}
          </Stack>

          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            justifyContent="space-between"
            alignItems={{ sm: 'center' }}
            spacing={1.5}
          >
            <FormControlLabel
              control={
                <Checkbox
                  checked={caseItem.continued}
                  onChange={handleContinuedChange}
                />
              }
              label="To be continued"
              sx={{ m: 0 }}
            />
            <Button onClick={() => onAddCharge(caseItem.id)}>
              {messages.addCharges || 'Add charge'}
            </Button>
          </Stack>

          {enableCaseReferences && (
            <CaseReferences
              caseId={caseItem.id}
              references={caseItem.caseReferences}
              config={config}
              onToggleReferencedCharge={onToggleReferencedCharge}
              onGenerateCharge={onGenerateChargeFromReference}
            />
          )}
        </Stack>
      </CardContent>
    </Card>
  );
};

export default CaseCard;
