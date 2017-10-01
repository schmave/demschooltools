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
                selectedClass: {name : "", required_minutes: 345}
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
        actionCreator.createClass(
            this.state.selectedClass._id,
            this.refs.name.getDOMNode().value,
            null,
            null,
            this.refs.required_minutes.getDOMNode().value,
        );
    },

    edit: function(selectedClass) {
        var s = jQuery.extend(this.state.selectedClass, selectedClass);
        this.setState(
            {selectedClass: s,
             creating: (!s._id)})
        this.refs.classEditor.show();
    },

    close: function () {
        if (this.refs.classEditor) {
            this.refs.classEditor.hide();
        }
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
                 <div>
                   <label htmlFor="required_minutes">Default Required Minutes:</label>
                   <div><input type="number" ref="required_minutes" id="required_minutes" onChange={this.handleChange}
                               value={this.state.selectedClass.required_minutes}/>
                   </div>
                 </div>
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
