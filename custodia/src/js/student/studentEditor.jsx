var React = require('react'),
    Router = require('react-router'),
    AdminItem = require('../adminwrapper.jsx'),
    DateTimePicker = require('react-widgets').DateTimePicker,
    actionCreator = require('../studentactioncreator'),
    Modal = require('../modal.jsx');

module.exports = React.createClass({
    getInitialState: function() {
        return {student: {start_date: null, guardian_email: "", name : "", olderdate: null}};
    },

    saveChange: function () {
        actionCreator.updateStudent(this.state.student._id,
                                    this.refs.name.getDOMNode().value,
                                    this.refs.startdate_datepicker.state.value,
                                    this.refs.email.getDOMNode().value);
        this.refs.studentEditor.hide();
    },

    edit: function(student) {
        this.setState({student: jQuery.extend({}, student)})
        this.refs.studentEditor.show();
    },

    toggleHours: function () {
        this.state.student.olderdate = !!!this.state.student.olderdate;
        actionCreator.toggleHours(this.state.student._id);
    },

    close: function () {
        if (this.refs.studentEditor) {
            this.refs.studentEditor.hide();
        }
    },

    handleChange(event) {
        const target = event.target;
        const value = target.type === 'checkbox' ? target.checked : target.value;
        const name = target.id;
        var partialState = this.state.student;
        partialState[name] = value;
        this.setState(partialState);
    },

    render: function()  {
        var pickerDate = (this.state.student.start_date) ? new Date(this.state.student.start_date) : null;
        return <div className="row">
          <Modal ref="studentEditor"
                 title="Edit Student">
            <form className="form">
              <div className="form-group" id="nameRow">
                <label htmlFor="name">Name:</label>
                <input ref="name" className="form-control" id="name"
                        onChange={this.handleChange}
                       value={this.state.student.name}/>
                <label htmlFor="email">Parent Email:</label>
                <input ref="email" className="form-control" id="guardian_email"
                        onChange={this.handleChange}
                       value={this.state.student.guardian_email}/>
                <div><input type="radio" id="older" onChange={this.toggleHours}
                            checked={!this.state.student.olderdate}/> 300 Minutes
                </div>
                <div><input type="radio" id="older" onChange={this.toggleHours}
                            checked={this.state.student.olderdate}/> 330 Minutes
                </div>
                <b>Student Start Date:</b>
                <DateTimePicker id="missing" defaultValue={pickerDate}
                                ref="startdate_datepicker"
                                calendar={true}
                                time={false} />
              </div>
              <button onClick={this.saveChange} className="btn btn-success">
                <i id="save-name" className="fa fa-check icon-large"> Save</i>
              </button>
              <button id="cancel-name" onClick={ this.close } className="btn btn-danger">
                <i className="fa fa-times"> Cancel</i>
              </button>
            </form>
          </Modal></div>;
    },

    _onChange: function() {
        if (this.refs.studentEditor) {
            this.refs.studentEditor.hide();
        }
    }
});
