import React from 'react';
import {
  Box,
  Button,
  Checkbox,
  FormControlLabel,
  Paper,
  Stack,
  Typography,
} from '../../components';

const CaseReferences = ({ caseId, references, config, onToggleReferencedCharge, onGenerateCharge }) => {
  const filteredReferences = (references || []).filter(
    (reference) => reference.id !== caseId,
  );

  if (filteredReferences.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
        No referenced cases selected.
      </Typography>
    );
  }

  return (
    <Stack spacing={2} sx={{ mt: 1 }}>
      {filteredReferences.map((reference) => (
        <Paper
          key={reference.id}
          sx={{
            p: 1.5,
            borderRadius: 1,
          }}
        >
          <Typography variant="subtitle2">{reference.caseNumber}</Typography>
          <Typography variant="body2" color="text.secondary">
            {reference.findings}
          </Typography>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            {reference.charges.map((charge) => {
              const chargeLabelPrefix = charge.is_sm_decision
                ? 'and School Meeting decided on'
                : 'and was assigned';
              const resolutionLabel = config.str_res_plan || 'resolution plan';

              return (
                <Box
                  key={`${reference.id}-${charge.charge_id}`}
                  sx={{
                    border: '1px dashed',
                    borderColor: charge.isReferenced ? 'success.light' : 'divider',
                    borderRadius: 1,
                    p: 1,
                  }}
                >
                  <FormControlLabel
                    control={
                      <Checkbox
                        size="small"
                        checked={charge.isReferenced}
                        onChange={(event) =>
                          onToggleReferencedCharge(
                            caseId,
                            charge.charge_id,
                            event.target.checked,
                          )
                        }
                      />
                    }
                    label={
                      <Typography variant="body2">
                        {charge.person} was charged with {charge.rule} {chargeLabelPrefix}{' '}
                        the {resolutionLabel} “{charge.resolutionPlan}”
                      </Typography>
                    }
                  />
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                    {!charge.has_generated ? (
                      <Button size="small" onClick={() => onGenerateCharge(caseId, charge)}>
                        Generate charge
                      </Button>
                    ) : (
                      <Typography variant="caption" color="success.main">
                        Charge generated
                      </Typography>
                    )}
                    {charge.previously_referenced_in_case && (
                      <Typography variant="caption" color="text.secondary">
                        Previously referenced in case {charge.previously_referenced_in_case}
                      </Typography>
                    )}
                  </Stack>
                </Box>
              );
            })}
          </Stack>
        </Paper>
      ))}
    </Stack>
  );
};

export default CaseReferences;
