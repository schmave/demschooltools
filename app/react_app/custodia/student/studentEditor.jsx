const React = require("react");
const dispatcher = require("../appdispatcher");
const constants = require("../appconstants");
const actionCreator = require("../studentactioncreator");
const Modal = require("../modal.jsx");
const dayjs = require("dayjs");

class StudentEditor extends React.Component {
  constructor(props) {
    super(props);
    this.modalRef = React.createRef();
    this.dispatchToken = dispatcher.register((action) => {
      if (
        action.type == constants.studentEvents.STUDENT_LOADED ||
        action.type == constants.studentEvents.ALL_LOADED
      ) {
        // Defer close() to avoid modifying state during dispatch
        setTimeout(() => this.close(), 0);
      }
    });

    this.state = {
      saving: false,
      student: { start_date: null },
      startdate_datepicker: null,
    };
  }

  componentWillUnmount = () => {
    if (this.dispatchToken) {
      dispatcher.unregister(this.dispatchToken);
      this.dispatchToken = null;
    }
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
    const { startdate_datepicker } = this.state;
    const requiredMinutes = parseInt(this.state.student.required_minutes, 10);
    actionCreator
      .updateStudent(
        this.state.student._id,
        this.formatDate(),
        Number.isNaN(requiredMinutes) ? 0 : requiredMinutes,
      )
      .always(this.savingHide);
  };

  edit = (student) => {
    const s = Object.assign({}, student);
    this.setState({
      student: s,
      startdate_datepicker: s.start_date ? dayjs(s.start_date) : null,
    });
    if (this.modalRef.current) {
      this.modalRef.current.show();
    }
  };

  close = () => {
    if (this.modalRef.current) {
      this.modalRef.current.hide();
    }
  };

  handleDateChange = (e) => {
    this.setState({ startdate_datepicker: e.target.value ? dayjs(e.target.value) : null });
  };

  handleChange = (event) => {
    const target = event.target;
    const value = target.type === "checkbox" ? target.checked : target.value;
    const name = target.id;
    const updatedStudent = { ...this.state.student, [name]: value };
    this.setState({ student: updatedStudent });
  };

  render() {
    return (
      <div className="row">
        <Modal ref={this.modalRef} title={`Edit ${this.state.student.name}`}>
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
                  value={this.state.student.required_minutes ?? ""}
                />
              </div>
              <div className="form-group">
                <label htmlFor="startdate">Student Start Date:</label>
                <input
                  type="date"
                  className="form-control"
                  id="startdate"
                  value={this.formatDate() || ""}
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

module.exports = StudentEditor;
