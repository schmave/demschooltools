var React = require('react'),
    Router = require('react-router'),
    AdminItem = require('../adminwrapper.jsx'),
    DateTimePicker = require('react-widgets').DateTimePicker,
    actionCreator = require('../studentactioncreator'),
    Modal = require('../modal.jsx');

module.exports = React.createClass({
    getInitialState: function() {
        return {};
    },
    _getMissingSwipe : function(student) {
        var missingdirection = (student.last_swipe_type == "in") ? "out" : "in";
        if (!student.in_today && student.direction == "out") {
            missingdirection = "in";
        }
        this.setState({missingdirection: missingdirection});
        this.refs.missingSwipeCollector.show();
        return missingdirection;
        // use ComponentDidUpdate to check the states then change them
        // use the onChange for the datepicker to mutate setState
    },

    _swipeWithMissing: function(missing) {
        var student = this.state.student,
            missing = this.refs.missing_datepicker.state.value;
        actionCreator.swipeStudent(student, student.direction, missing);
        this.setState({student: {}, missingdirection: false})
        this.refs.missingSwipeCollector.hide();
    },

    saveChange: function () {
        actionCreator.updateStudent(this.state.student._id,
                                    this.refs.name.getDOMNode().value,
                                    this.refs.missing_datepicker.state.value,
                                    this.refs.email.getDOMNode().value);
        this.toggleEdit();
    },

    editingStudentName: function () {
    },

    edit: function(student) {
        this.setState({student: student})
        student.direction = direction;
        var missing_in = ((student.last_swipe_type == "out"
                        || (student.last_swipe_type == "in" && !student.in_today)
                        || !student.last_swipe_type)
                       && direction == "out"),
            missing_out = (student.last_swipe_type == "in"
                        && direction == "in");

        if ((missing_in || missing_out) && student.last_swipe_date) {
            var missingD = this._getMissingSwipe(student);
            this._setCalendarTime(student, missingD);
        } else {
            actionCreator.swipeStudent(student, direction);
        }
    },

    toggleHours: function () {
        this.state.student.olderdate = !!!this.state.student.olderdate;
        actionCreator.toggleHours(this.state.student._id);
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
                <button id="cancel-name" onClick={this.toggleEdit} className="btn btn-danger">
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
        if (this.refs.missingSwipeCollector) {
            this.refs.missingSwipeCollector.hide();
        }
    }
});
