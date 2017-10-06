var React = require('react'),
    Router = require('react-router'),
    AdminItem = require('../adminwrapper.jsx'),
    dispatcher = require('../appdispatcher'),
    constants = require('../appconstants'),
    DateTimePicker = require('react-widgets').DateTimePicker,
    actionCreator = require('../studentactioncreator'),
    Modal = require('../modal.jsx');

module.exports = React.createClass({
    getInitialState: function() {
        var that = this;
        dispatcher.register(function(action){
            if (action.type == constants.studentEvents.STUDENT_LOADED
                || action.type == constants.studentEvents.ALL_LOADED) {
                that.savingHide();
                that.close();
            }
        });
        return {saving: false,
                student: {start_date: null, guardian_email: "", name : "", olderdate: null},
                startdate_datepicker: new Date()};
    },

    savingShow: function () {
        this.setState({saving:true});
    },
    savingHide: function () {
        this.setState({saving:false});
    },

    saveChange: function () {
        this.savingShow();
        var startDate = this.refs.startdate.props.value || new Date();
        if (this.state.student._id == null) {
            actionCreator.createStudent(this.refs.name.getDOMNode().value,
                                        startDate,
                                        this.refs.email.getDOMNode().value,
                                        this.refs.minutes.getDOMNode().value,
                                        this.state.student.is_teacher);
        } else {
            actionCreator.updateStudent(this.state.student._id,
                                        this.refs.name.getDOMNode().value,
                                        startDate,
                                        this.refs.email.getDOMNode().value,
                                        this.refs.minutes.getDOMNode().value,
                                        this.state.student.is_teacher);
        }
    },

    edit: function(student) {
        var s = jQuery.extend({}, student);
        this.setState(
            {student: s,
             creating: (!s._id),
             startdate_datepicker: (s.start_date) ? new Date(s.start_date) : new Date()})
        this.refs.studentEditor.show();
    },

    close: function () {
        if (this.refs.studentEditor) {
            this.refs.studentEditor.hide();
        }
    },

    handleTeacherChange: function(state) {
        this.state.student.is_teacher = !this.state.student.is_teacher;
        this.setState({student: this.state.student});
    },

    handleDateChange: function(d) {
        this.setState({startdate_datepicker: d});
    },

    handleChange: function(event) {
        const target = event.target;
        const value = target.type === 'checkbox' ? target.checked : target.value;
        const name = target.id;
        var partialState = this.state.student;
        partialState[name] = value;
        this.setState(partialState);
    },

    render: function()  {
        var title = ((this.state.creating) ? "Create" :  "Edit") + " Student"
        return <div className="row">
          <Modal ref="studentEditor"
                 title={title}>
            {(this.state.saving) ?
            <div>
              <p style={{'text-align':'center'}}>
                <img src="/images/spinner.gif" />
              </p>
            </div>
             : <form className="form">
                <div className="form-group" id="nameRow">
                <label htmlFor="name">Name:</label>
                <input ref="name" className="form-control" id="name"
                onChange={this.handleChange}
                value={this.state.student.name}/>
                <label htmlFor="email">Parent Email:</label>
                <input ref="email" className="form-control" id="guardian_email"
                onChange={this.handleChange}
                value={this.state.student.guardian_email}/>

                <label htmlFor="minutes">Required Minutes:</label>
                <input ref="minutes" className="form-control" id="minutes"
                onChange={this.handleChange}
                value={this.state.student.required_minutes}/>

                <div>
                  <label htmlFor="is_teacher">Is Teacher:</label>
                  <div><input type="checkbox" id="is_teacher" onChange={this.handleTeacherChange}
                              checked={this.state.student.is_teacher}/> Is Teacher?
                  </div>
                </div>
                <b>Student Start Date:</b>
                <DateTimePicker id="missing" value={this.state.startdate_datepicker}
                                ref="startdate" onChange={this.handleDateChange}
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
            }
          </Modal></div>;
    },

    _onChange: function() {
        if (this.refs.studentEditor) {
            this.refs.studentEditor.hide();
        }
    }
});
