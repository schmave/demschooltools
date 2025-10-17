import { Box, Modal as MuiModal } from "@mui/material";
import React from "react";

class Modal extends React.Component {
  render() {
    const modalStyle = {
      position: "absolute",
      top: "50%",
      left: "50%",
      transform: "translate(-50%, -50%)",
      width: 600,
      height: 450,
      bgcolor: "background.paper",
      border: "1px solid #ddd",
      borderRadius: "4px",
      boxShadow: 24,
      outline: "none",
    };

    const headerStyle = {
      backgroundColor: "#337ab7",
      color: "white",
      padding: "10px 15px",
      borderBottom: "1px solid #ddd",
      borderRadius: "4px 4px 0 0",
      margin: 0,
      fontSize: "16px",
      fontWeight: "bold",
    };

    const bodyStyle = {
      padding: "15px",
      height: "calc(100% - 50px)", // Subtract header height
      overflow: "auto",
    };

    return (
      <MuiModal
        open={this.props.open}
        onClose={this.props.onClose}
        aria-labelledby="modal-title"
        aria-describedby="modal-description"
      >
        <Box sx={modalStyle}>
          <div style={headerStyle} id="modal-title">
            {this.props.title}
          </div>
          <div style={bodyStyle}>{this.props.children}</div>
        </Box>
      </MuiModal>
    );
  }
}

export default Modal;
