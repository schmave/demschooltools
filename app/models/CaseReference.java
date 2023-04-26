package models;

import controllers.Application;

import java.util.ArrayList;
import java.util.List;

public class CaseReference {

	public Integer id;
	public String case_number;
	public String findings;
	public List<ChargeReference> charges;

	public static List<CaseReference> create(Case referencing_case, Organization org) {
		List<CaseReference> results = new ArrayList<>();
		
		for (Case referenced_case : referencing_case.referenced_cases) {
			
			CaseReference result = new CaseReference();
			result.id = referenced_case.id;
			result.case_number = referenced_case.case_number;
			result.findings = Application.generateCompositeFindingsFromCaseReferences(referenced_case);
			result.charges = new ArrayList<>();

			for (Charge charge : referenced_case.charges) {

				if (charge.person == null) continue;

				ChargeReference cr = new ChargeReference();
				
				cr.charge_id = charge.id;
				cr.person = charge.person.getDisplayName();
				cr.rule = charge.getRuleTitle();
				cr.resolution_plan = charge.resolution_plan;
				cr.is_referenced = referencing_case.referenced_charges.contains(charge);

				if (charge.sm_decision != null && !charge.sm_decision.isEmpty()) {
					cr.is_sm_decision = true;
					cr.resolution_plan = charge.sm_decision;
				}

				if (charge.referred_to_sm && cr.resolution_plan.isEmpty()) {
					cr.resolution_plan = "[Referred to School Meeting]";
				}

				for (Charge new_charge : referencing_case.charges) {
					if (new_charge.referenced_charge == charge) {
						cr.has_generated = true;
						cr.generated_charge_id = new_charge.id;
						cr.has_default_rule = new_charge.rule != null && new_charge.rule.id.equals(
								Entry.findBreakingResPlanEntryId(org));
					}
				}

				if (!cr.has_generated && charge.referencing_charges.size() > 0) {
					cr.previously_referenced_in_case = charge.referencing_charges.get(0).the_case.case_number;
				}

				result.charges.add(cr);
			}

			results.add(result);
		}
		return results;
	}
}
