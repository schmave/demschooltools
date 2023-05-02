package models;

import controllers.Application;
import java.util.ArrayList;
import java.util.List;

public class CaseReference {

  public Integer id;
  public String caseNumber;
  public String findings;
  public List<ChargeReference> charges;

  public static List<CaseReference> create(Case referencing_case, Organization org) {
    List<CaseReference> results = new ArrayList<>();

    for (Case referenced_case : referencing_case.referenced_cases) {

      CaseReference result = new CaseReference();
      result.id = referenced_case.getId();
      result.caseNumber = referenced_case.getCaseNumber();
      result.findings = Application.generateCompositeFindingsFromCaseReferences(referenced_case);
      result.charges = new ArrayList<>();

      for (Charge charge : referenced_case.charges) {

        if (charge.getPerson() == null) continue;

        ChargeReference cr = new ChargeReference();

        cr.charge_id = charge.getId();
        cr.person = charge.getPerson().getDisplayName();
        cr.rule = charge.getRuleTitle();
        cr.resolutionPlan = charge.getResolutionPlan();
        cr.isReferenced = referencing_case.referenced_charges.contains(charge);

        if (charge.getSmDecision() != null && !charge.getSmDecision().isEmpty()) {
          cr.is_sm_decision = true;
          cr.resolutionPlan = charge.getSmDecision();
        }

        if (charge.getReferredToSm() && cr.resolutionPlan.isEmpty()) {
          cr.resolutionPlan = "[Referred to School Meeting]";
        }

        for (Charge new_charge : referencing_case.charges) {
          if (new_charge.getReferencedCharge() == charge) {
            cr.has_generated = true;
            cr.generated_charge_id = new_charge.getId();
            cr.has_default_rule =
                new_charge.getRule() != null
                    && new_charge.getRule().getId().equals(Entry.findBreakingResPlanEntryId(org));
          }
        }

        if (!cr.has_generated && charge.referencing_charges.size() > 0) {
          cr.previously_referenced_in_case =
              charge.referencing_charges.get(0).getTheCase().getCaseNumber();
        }

        result.charges.add(cr);
      }

      results.add(result);
    }
    return results;
  }
}
