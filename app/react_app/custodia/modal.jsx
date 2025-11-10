const React = require("react");
const $ = require("jquery");

const overlayStyles = {
  position: "fixed",
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: "rgba(0, 0, 0, 0.75)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  zIndex: 1040,
};

const baseDialogStyles = {
  width: "600px",
  maxWidth: "90%",
  maxHeight: "90%",
  height: "450px",
  backgroundColor: "#fff",
  borderRadius: "4px",
  overflow: "hidden",
  boxShadow: "0 5px 15px rgba(0, 0, 0, 0.5)",
  display: "flex",
  flexDirection: "column",
};

module.exports = class extends React.Component {
  static displayName = "Modal";

  state = { isVisible: false };

  componentWillUnmount() {
    this.unbindEsc();
  }

  show = () => {
    this.setState({ isVisible: true }, () => {
      $(document.body).off("keydown", this.handleKeyDown);
      $(document.body).on("keydown", this.handleKeyDown);
    });
  };

  hide = () => {
    this.setState({ isVisible: false }, this.unbindEsc);
  };

  handleKeyDown = (keypress) => {
    if (keypress.keyCode === 27) {
      this.hide();
    }
  };

  unbindEsc = () => {
    $(document.body).off("keydown", this.handleKeyDown);
  };

  onOverlayClick = (event) => {
    if (event.target === event.currentTarget) {
      this.hide();
    }
  };

  render() {
    if (!this.state.isVisible) {
      return null;
    }

    const dialogStyles = Object.assign({}, baseDialogStyles, this.props.dialogStyles || {});

    return (
      <div style={overlayStyles} onClick={this.onOverlayClick}>
        <div className="inner-large-content panel panel-primary" style={dialogStyles}>
          <div className="panel-heading">{this.props.title}</div>
          <div className="panel-body" style={{ overflowY: "auto" }}>
            {this.props.children}
          </div>
        </div>
      </div>
    );
  }
};
