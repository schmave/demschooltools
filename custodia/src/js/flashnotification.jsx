var React = require("react"),
  store = require("./flashnotificationstore"),
  // Notification = require("react-notification-system"),
  constants = require("./appconstants");

class FlashNotification extends React.Component {
  state = { showing: false };

  componentDidMount() {
    store.addChangeListener(this._onChange);
    this.notifications = this.refs.notifications;
  }

  _onChange = () => {
    var message = store.getLatest();
    if (message) {
      this.notifications.addNotification({
        message: message.message,
        level: message.level,
      });
    }
  };

  render() {
    return null;
    // return <Notification ref="notifications" />;
  }
}

module.exports = FlashNotification;
