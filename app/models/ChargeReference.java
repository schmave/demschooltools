package models;

public class ChargeReference {

	public Integer charge_id;
	public String person;
	public String rule;
	public String resolution_plan;
	public boolean is_sm_decision;
	public boolean is_referenced;
	public boolean has_generated;
	public Integer generated_charge_id;
	public boolean has_default_rule;
	public String previously_referenced_in_case;
}
