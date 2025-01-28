const React = require("react");
const ReactDOM = require("react-dom");
const dispatcher = require("../appdispatcher");
const constants = require("../appconstants");
const actionCreator = require("../studentactioncreator");
const Modal = require("../modal.jsx");
const moment = require("moment");

module.exports = class extends React.Component {
  static displayName = "StudentEditor";

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

  saveChange = (e) => {
    e.preventDefault();
    this.savingShow();
    const { startdate_datepicker } = this.state;
    const timestamp = startdate_datepicker
      ? moment(startdate_datepicker).format("YYYY-MM-DD")
      : null;
    actionCreator
      .updateStudent(
        this.state.student._id,
        timestamp,
        parseInt(ReactDOM.findDOMNode(this.refs.required_minutes).value, 10),
      )
      .always(this.savingHide);
  };

  edit = (student) => {
    const s = jQuery.extend({}, student);
    this.setState({
      student: s,
      startdate_datepicker: s.start_date ? new Date(s.start_date + "T00:00:00") : null,
    });
    this.refs.studentEditor.show();
  };

  close = () => {
    if (this.refs.studentEditor) {
      this.refs.studentEditor.hide();
    }
  };

  handleDateChange = (d) => {
    this.setState({ startdate_datepicker: d });
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
        <Modal ref="studentEditor" title={`Edit ${this.state.student.name}`}>
          {this.state.saving ? (
            <div>
              <p style={{ textAlign: "center" }}>
                <img src="/static/images/spinner.gif" />
              </p>
            </div>
          ) : (
            <form className="form">
              <p>To change this student's name, visit the People tab in DemSchoolTools.</p>
              <div className="form-group">
                <label htmlFor="required_minutes">Required Minutes:</label>
                <input
                  ref="required_minutes"
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
                  id="startdate"
                  value={this.state.startdate_datepicker}
                  onChange={this.handleDateChange}
                  date={true}
                  time={false}
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

  _onChange = () => {
    if (this.refs.studentEditor) {
      this.refs.studentEditor.hide();
    }
  };
};
