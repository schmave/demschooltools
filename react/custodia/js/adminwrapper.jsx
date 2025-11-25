import React from "react";

import userStore from "./userstore";

class AdminWrapper extends React.Component {
  state = { permitted: userStore.isAdmin() };

  componentDidMount() {
    userStore.addChangeListener(this._onChange);
  }

  componentWillUnmount() {
    userStore.removeChangeListener(this._onChange);
  }

  render() {
    if (this.state.permitted) {
      return this.props.children;
    }

    return <div />;
  }

  _onChange = () => {
    this.setState({ permitted: userStore.isAdmin() });
  };
}

export default AdminWrapper;
