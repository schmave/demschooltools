const React = require("react");
const store = require("./flashnotificationstore");

const ALERT_LEVELS = {
  success: "alert-success",
  error: "alert-danger",
  warning: "alert-warning",
  info: "alert-info",
};

class FlashNotification extends React.Component {
  state = { current: null };

  componentDidMount() {
    store.addChangeListener(this._onChange);
  }

  componentWillUnmount() {
    store.removeChangeListener(this._onChange);
    this.clearHideTimer();
  }

  clearHideTimer() {
    if (this.hideTimer) {
      clearTimeout(this.hideTimer);
      this.hideTimer = null;
    }
  }

  _onChange = () => {
    const message = store.getLatest();
    if (message && message.message) {
      this.clearHideTimer();
      this.setState(
        {
          current: {
            id: Date.now(),
            text: message.message,
            level: message.level || "success",
          },
        },
        () => {
          this.hideTimer = setTimeout(() => {
            this.setState({ current: null });
            this.hideTimer = null;
          }, 4000);
        },
      );
    }
  };

  render() {
    if (!this.state.current) {
      return null;
    }
    const alertClass = ALERT_LEVELS[this.state.current.level] || ALERT_LEVELS.success;
    return (
      <div className={`alert ${alertClass} flash-notification`} role="alert">
        {this.state.current.text}
      </div>
    );
  }
}

module.exports = FlashNotification;
