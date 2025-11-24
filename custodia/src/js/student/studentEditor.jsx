import dayjs from "dayjs";
import React from "react";

import constants from "../appconstants.js";
import dispatcher from "../appdispatcher.js";
import Modal from "../modal.jsx";
import * as actionCreator from "../studentactioncreator.js";

class StudentEditor extends React.Component {
  constructor(props) {
    super(props);
    this.dispatchToken = dispatcher.register((action) => {
      if (
        action.type == constants.studentEvents.STUDENT_LOADED ||
        action.type == constants.studentEvents.ALL_LOADED
      ) {
        this.close();
      }
    });

    this.state = {
      saving: false,
      student: { start_date: null },
      startdate_datepicker: null,
      show_modal: false,
    };
  }

  componentWillUnmount = () => {
    dispatcher.unregister(this.dispatchToken);
  };

  savingShow = () => {
    this.setState({ saving: true });
  };

  savingHide = () => {
    this.setState({ saving: false });
  };

  formatDate = () => {
    const { startdate_datepicker } = this.state;
    return startdate_datepicker ? startdate_datepicker.format("YYYY-MM-DD") : null;
  };

  saveChange = (e) => {
    e.preventDefault();
    this.savingShow();
    actionCreator
      .updateStudent(
        this.state.student._id,
        this.formatDate(),
        parseInt(this.state.student.required_minutes, 10),
      )
      .finally(this.savingHide);
  };

  edit = (student) => {
    const s = Object.assign({}, student);
    this.setState({
      student: s,
      startdate_datepicker: s.start_date ? dayjs(s.start_date) : null,
      show_modal: true,
    });
  };

  close = () => {
    this.setState({ show_modal: false });
  };

  handleDateChange = (e) => {
    this.setState({ startdate_datepicker: e.target.value ? dayjs(e.target.value) : null });
  };

  handleChange = (event) => {
    const target = event.target;
    const value = target.type === "checkbox" ? target.checked : target.value;
    const name = target.id;
    const partialState = this.state.student;
    partialState[name] = value;
    this.setState(partialState);
  };

  render() {
    return (
      <div className="row">
        <Modal
          open={this.state.show_modal}
          onClose={this.close}
          title={`Edit ${this.state.student.name}`}
        >
          {this.state.saving ? (
            <div>
              <p style={{ textAlign: "center" }}>
                <img src="/django-static/images/spinner.gif" />
              </p>
            </div>
          ) : (
            <form className="form">
              <p>To change this student's name, visit the People tab in DemSchoolTools.</p>
              <div className="form-group">
                <label htmlFor="required_minutes">Required Minutes:</label>
                <input
                  className="form-control"
                  id="required_minutes"
                  type="number"
                  onChange={this.handleChange}
                  value={this.state.student.required_minutes}
                />
              </div>
              <div className="form-group">
                <label htmlFor="startdate">Student Start Date:</label>
                <input
                  type="date"
                  className="form-control"
                  id="startdate"
                  defaultValue={this.formatDate()}
                  onChange={this.handleDateChange}
                />
              </div>
              <button onClick={this.saveChange} type="submit" className="btn btn-success">
                <i id="save-name" className="fa fa-check icon-large"></i> Save
              </button>{" "}
              <button id="cancel-name" onClick={this.close} className="btn btn-danger">
                <i className="fa fa-times"></i> Cancel
              </button>
            </form>
          )}
        </Modal>
      </div>
    );
  }
}

export default StudentEditor;
