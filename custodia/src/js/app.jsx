var React = require('react'),
    Nav = require('./nav.jsx'),
    StudentTable = require('./studenttable.jsx');

React.render(<Nav />, document.getElementById('nav'));
React.render(<StudentTable />, document.getElementById('content'));