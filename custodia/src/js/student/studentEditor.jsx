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
                                    this.refs.missing_datepicker.state.value,
                                    this.refs.email.getDOMNode().value);
        this.refs.studentEditor.hide();
    },

    edit: function(student) {
        this.setState({student: student})
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

    render: function()  {
        var pickerDate = (this.state.student.start_date) ? new Date(this.state.student.start_date) : null;
        return <div className="row">
          <Modal ref="studentEditor"
                 title="Edit Student">
            <form className="form-inline">
              <div className="col-sm-3" id="nameRow">
                Name: <input ref="name" className="form-control" id="studentName"
                             defaultValue={this.state.student.name}/>
                Parent Email: <input ref="email" className="form-control" id="email"
                                     defaultValue={this.state.student.guardian_email}/>
                <button onClick={this.saveChange} className="btn btn-success">
                  <i id="save-name" className="fa fa-check icon-large">Save</i></button>
                <button id="cancel-name" onClick={ this.close() } className="btn btn-danger">
                  <i className="fa fa-times"></i></button>
              </div>
              <div className="col-md-4" >
                <div><input type="radio" name="older" onChange={this.toggleHours}
                            checked={!this.state.student.olderdate}/> 300 Minutes
                </div>
                <div><input type="radio" name="older" onChange={this.toggleHours}
                            checked={this.state.student.olderdate}/> 330 Minutes
                </div>
              </div>
              <div className="col-md-4" id="nameRow">
                <b>Student Start Date:</b>
                <DateTimePicker id="missing" defaultValue={pickerDate}
                                ref="missing_datepicker"
                                calendar={true}
                                time={false} />
              </div>
            </form>
          </Modal></div>;
    },

    _onChange: function() {
        if (this.refs.studentEditor) {
            //this.refs.studentEditor.hide();
        }
    }
});
