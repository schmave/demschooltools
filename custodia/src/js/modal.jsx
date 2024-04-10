var React = require("react"),
  Skylight = require("react-skylight").default;

module.exports = class extends React.Component {
  static displayName = "Modal";

  show = () => {
    this.refs.modal.show();
    $(document.body).off("keydown");
    $(document.body).on("keydown", this.handleKeyDown);
  };

  hide = () => {
    this.refs.modal.hide();
  };

  handleKeyDown = (keypress) => {
    if (keypress.keyCode == 27 /*esc*/) {
      this.hide();
      this.unbindEsc();
    }
  };

  unbindEsc = () => {
    $(document.body).off("keydown");
  };

  componentWillUnMount = () => {
    this.unbindEsc();
  };

  render() {
    return (
      <Skylight
        ref="modal"
        dialogStyles={{ height: "450px", backgroundColor: "", boxShadow: "" }}
      >
        <div
          className="inner-large-content panel panel-primary"
          style={{ height: "100%" }}
        >
          <div className="panel-heading">{this.props.title}</div>
          <div className="panel-body">{this.props.children}</div>
        </div>
      </Skylight>
    );
  }
};
