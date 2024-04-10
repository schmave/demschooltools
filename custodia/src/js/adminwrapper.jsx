var userStore = require('./userstore'),
React = require('react');

class AdminWrapper extends React.Component {
    state = {permitted: userStore.isAdmin()};

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

        return <div/>;
    }

    _onChange = () => {
        this.setState({permitted: userStore.isAdmin()});
    };
}

module.exports = AdminWrapper;
