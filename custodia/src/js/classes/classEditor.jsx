var React = require('react'),
    Router = require('react-router'),
    AdminItem = require('../adminwrapper.jsx'),
    dispatcher = require('../appdispatcher'),
    constants = require('../appconstants'),
    DateTimePicker = require('react-widgets').DateTimePicker,
    actionCreator = require('../classactioncreator'),
    Modal = require('../modal.jsx');

module.exports = React.createClass({
    getInitialState: function() {
        var that = this;
        dispatcher.register(function(action){
            if (action.type == constants.classEvents.CLASS_CREATED
           //     || action.type == constants.classEvents.ALL_LOADED
            ) {
                that.savingHide();
                that.close();
            }
        });
        return {saving: false,
                _class: {to_date: null, from_date: null, name : "", required_minutes: 345},
                todate_datepicker: new Date(),
                fromdate_datepicker: new Date(),
                selectedClass: {}
        };
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
        /* actionCreator.createClass(
         *     this.state.student._id == null,
         *     this.refs.name.getDOMNode().value,
         *     startDate,
         *     this.refs.email.getDOMNode().value,
         *     this.state.student.is_teacher);*/
    },

    edit: function(selectedClass) {
        var s = jQuery.extend({}, selectedClass);
        this.setState(
            {selectedClass: s,
             creating: (!s._id),
             startdate_datepicker: (s.start_date) ? new Date(s.start_date) : new Date()})
        this.refs.classEditor.show();
    },

    toggleHours: function () {
        if (this.state.selectedClass._id > 0) {
            this.state.selectedClass.olderdate = !!!this.state.selectedClass.olderdate;
            actionCreator.toggleHours(this.state.selectedClass._id);
        }
    },

    close: function () {
        if (this.refs.classEditor) {
            this.refs.classEditor.hide();
        }
    },

    handleTeacherChange: function(state) {
        this.state.selectedClass.is_teacher = !this.state.selectedClass.is_teacher;
        this.setState({selectedClass: this.state.selectedClass});
    },

    handleDateChange: function(d) {
        this.setState({startdate_datepicker: d});
    },

    handleChange: function(event) {
        const target = event.target;
        const value = target.type === 'checkbox' ? target.checked : target.value;
        const name = target.id;
        var partialState = this.state.selectedClass;
        partialState[name] = value;
        this.setState(partialState);
    },

    render: function()  {
        var title = ((this.state.creating) ? "Create" :  "Edit") + " Class"
        return <div className="row">
          <Modal ref="classEditor"
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
                        value={this.state.selectedClass.name}/>
                 <label htmlFor="email">Parent Email:</label>
                 <input ref="email" className="form-control" id="guardian_email"
                        onChange={this.handleChange}
                        value={this.state.selectedClass.guardian_email}/>

                 { (!this.state.creating) ?
                   <div>
                     <label htmlFor="email">Required Hours:</label>
                     <div><input type="radio" id="older" onChange={this.toggleHours}
                                 checked={!this.state.selectedClass.olderdate}/> 300 Minutes
                     </div>
                     <div><input type="radio" id="older" onChange={this.toggleHours}
                                 checked={this.state.selectedClass.olderdate}/> 330 Minutes
                     </div>
                   </div>
                   : <div></div>
                 }
                   <div>
                     <label htmlFor="is_teacher">Is Teacher:</label>
                     <div><input type="checkbox" id="is_teacher" onChange={this.handleTeacherChange}
                                 checked={this.state.selectedClass.is_teacher}/> Is Teacher?
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
        if (this.refs.classEditor) {
            this.refs.classEditor.hide();
        }
    }
});
