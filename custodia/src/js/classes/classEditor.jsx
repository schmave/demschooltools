import React, { Component } from "react";
import { findDOMNode } from "react-dom";

import { classEvents } from "../appconstants";
import dispatcher from "../appdispatcher";
import { createClass } from "../classactioncreator";
import Modal from "../modal.jsx";

export default class extends Component {
  static displayName = "ClassEditor";

  constructor(props, context) {
    super(props, context);
    var that = this;
    dispatcher.register(function (action) {
      if (
        action.type == classEvents.CLASS_CREATED
        //     || action.type == constants.classEvents.ALL_LOADED
      ) {
        that.savingHide();
        that.close();
      }
    });

    this.state = {
      saving: false,
      selectedClass: { name: "", required_minutes: 345 },
    };
  }

  savingShow = () => {
    this.setState({ saving: true });
  };

  savingHide = () => {
    this.setState({ saving: false });
  };

  saveChange = () => {
    this.savingShow();
    createClass(
      this.state.selectedClass._id,
      findDOMNode(this.refs.name).value,
      null,
      null,
      findDOMNode(this.refs.required_minutes).value,
      "10:30:00",
      // ReactDOM.findDOMNode(this.refs.late_time).value
    );
  };

  edit = (selectedClass) => {
    var s = jQuery.extend(this.state.selectedClass, selectedClass);
    this.setState({ selectedClass: s, creating: !s._id });
    this.refs.classEditor.show();
  };

  close = () => {
    if (this.refs.classEditor) {
      this.refs.classEditor.hide();
    }
  };

  handleChange = (event) => {
    const target = event.target;
    const value = target.type === "checkbox" ? target.checked : target.value;
    const name = target.id;
    var partialState = this.state.selectedClass;
    partialState[name] = value;
    this.setState(partialState);
  };

  render() {
    var title = (this.state.creating ? "Create" : "Edit") + " Class";
    return (
      <div className="row">
        <Modal ref="classEditor" title={title}>
          {this.state.saving ? (
            <div>
              <p style={{ textAlign: "center" }}>
                <img src="/images/spinner.gif" />
              </p>
            </div>
          ) : (
            <form className="form">
              <div className="form-group" id="nameRow">
                <label htmlFor="name">Name:</label>
                <input
                  ref="name"
                  className="form-control"
                  id="name"
                  onChange={this.handleChange}
                  value={this.state.selectedClass.name}
                />
                <div>
                  <label htmlFor="required_minutes">Default Required Minutes:</label>
                  <div>
                    <input
                      type="number"
                      ref="required_minutes"
                      id="required_minutes"
                      onChange={this.handleChange}
                      value={this.state.selectedClass.required_minutes}
                    />
                  </div>
                </div>

                <label htmlFor="name">Name:</label>
                <input
                  ref="name"
                  className="form-control"
                  id="name"
                  onChange={this.handleChange}
                  value={this.state.selectedClass.name}
                />
                {/* <div>
                 <label htmlFor="late_time">Highlight student arrival after:</label>
                 <div>
                 <DateTimePicker id="missing" value={this.state.selectedClass.late_time}
                 parse={['HH:mm:ss']}
                 ref="late_time" onChange={this.handleChange}
                 calendar={false} time={true} />
                 </div>
                 </div> */}
              </div>

              <button onClick={this.saveChange} className="btn btn-success">
                <i id="save-name" className="fa fa-check icon-large">
                  {" "}
                  Save
                </i>
              </button>
              <button id="cancel-name" onClick={this.close} className="btn btn-danger">
                <i className="fa fa-times"> Cancel</i>
              </button>
            </form>
          )}
        </Modal>
      </div>
    );
  }

  _onChange = () => {
    if (this.refs.classEditor) {
      this.refs.classEditor.hide();
    }
  };
}
