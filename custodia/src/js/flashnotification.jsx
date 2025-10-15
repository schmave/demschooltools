import { Component } from "react";
import { addChangeListener, getLatest } from "./flashnotificationstore.js";
// import { Notification } from "react-notification-system";

class FlashNotification extends Component {
  state = { showing: false };

  componentDidMount() {
    addChangeListener(this._onChange);
    this.notifications = this.refs.notifications;
  }

  _onChange = () => {
    var message = getLatest();
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

export default FlashNotification;
