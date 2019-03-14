var userStore = require('./userstore'),
React = require('react');

class SuperWrapper extends React.Component {
    state = {permitted: userStore.isSuper()};

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

        return null;
    }

    _onChange = () => {
        this.setState({permitted: userStore.isSuper()});
    };
}

module.exports = SuperWrapper;
