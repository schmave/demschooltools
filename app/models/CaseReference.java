package models;

import models.*;
import java.util.*;

public class CaseReference {

	public String case_number;
	public String findings;
	public List<ChargeReference> charges;

	public static List<CaseReference> create(Case referencing_case) {
		List<CaseReference> results = new ArrayList<CaseReference>();
		
		for (Case referenced_case : referencing_case.referenced_cases) {
			
			CaseReference result = new CaseReference();
			result.case_number = referenced_case.case_number;
			result.findings = referenced_case.findings;
			result.charges = new ArrayList<ChargeReference>();

			for (Charge charge : referenced_case.charges) {

				if (charge.person == null) continue;

				ChargeReference cr = new ChargeReference();
				cr.charge_id = charge.id;
				cr.person = charge.person.display_name;
				cr.rule = charge.getRuleTitle();
				cr.resolution_plan = charge.rp_text;

				cr.is_referenced = referencing_case.referenced_charges.contains(charge);

				for (Charge new_charge : referencing_case.charges) {
					if (new_charge.referenced_charge == charge) {
						cr.has_generated = true;
					}
				}

				result.charges.add(cr);
			}

			results.add(result);
		}
		return results;
	}
}
