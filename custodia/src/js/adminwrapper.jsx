var userStore = require('./userstore'),
React = require('react');

var exports = React.createClass({
    getInitialState: function(){
        return {permitted: userStore.isAdmin()};
    },
    componentDidMount: function(){
      userStore.addChangeListener(this._onChange);
    },
    componentWillUnmount: function(){
      userStore.removeChangeListener(this._onChange);
    },
    render: function () {
        if (this.state.permitted) {
            return this.props.children;
        }

        return null;
    },
    _onChange: function(){
        this.setState({permitted: userStore.isAdmin()});
    }
});

module.exports = exports;