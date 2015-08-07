var userStore = require('./userstore')
    React = require('react');

var exports = React.createClass({
    permitted: function () {
        return userStore.isAdmin();
        },

    render: function () {
        if (this.permitted()) {
            return this.props.children;
        }

        return null;
    }
});

module.exports = exports;