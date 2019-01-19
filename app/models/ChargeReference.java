package models;

public class ChargeReference {

	public Integer charge_id;
	public String person;
	public String rule;
	public String rp_text;
	public ResolutionPlanType rp_type;
	public boolean is_referenced;
	public boolean has_generated;
	public Integer generated_charge_id;
	public boolean has_default_rule;
}
