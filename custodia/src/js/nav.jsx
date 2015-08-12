var React = require('react'),
    Router = require('react-router'),
    Link = Router.Link,
    AdminWrapper = require('./adminwrapper.jsx');

module.exports = React.createClass({
    render: function () {
        return <nav className="navbar navbar-inverse navbar-fixed-top" role="navigation">
            <div className="container">
                <div className="navbar-header">
                    <button type="button" className="navbar-toggle collapsed" data-toggle="collapse"
                            data-target="#navbar"
                            aria-expanded="false" aria-controls="navbar">
                        <span className="sr-only">Toggle navigation</span>
                        <span className="icon-bar"></span>
                        <span className="icon-bar"></span>
                        <span className="icon-bar"></span>
                    </button>
                    <a className="navbar-brand" href="/">Overseer</a>
                </div>
                <div id="navbar" className="collapse navbar-collapse">
                    <ul className="nav navbar-nav">
                        <li><Link to="students">Home</Link></li>
                        <AdminWrapper>
                            <li><Link to="reports">Student Totals</Link></li>
                        </AdminWrapper>
                        <li><a href="/logout">Logout</a></li>
                    </ul>
                </div>
            </div>
        </nav>;
    }
});
