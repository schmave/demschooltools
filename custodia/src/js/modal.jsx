import React from "react";
import ReactResponsiveModal from "react-responsive-modal";

class Modal extends React.Component {
  render() {
    return (
      <ReactResponsiveModal
        center
        open={this.props.open}
        onClose={this.props.onClose}
        styles={{ modal: { height: "450px" } }}
      >
        <div className="inner-large-content panel panel-primary" style={{ height: "100%" }}>
          <div className="panel-heading">{this.props.title}</div>
          <div className="panel-body">{this.props.children}</div>
        </div>
      </ReactResponsiveModal>
    );
  }
}

export default Modal;
