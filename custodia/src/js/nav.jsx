var React = require('react'),
    Router = require('react-router'),
    Link = Router.Link,
    SuperWrapper = require('./superwrapper.jsx'),
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
                    <Link to="students" id="home" className="navbar-brand">Custodia</Link>
                </div>
                <div id="navbar" className="collapse navbar-collapse">
                    <ul className="nav navbar-nav">
                        <AdminWrapper>
                            <li><Link id="totals-link" to="reports">Reports</Link></li>
                        </AdminWrapper>
                        <AdminWrapper>
                            <li><Link id="students-link" to="studentAdmin">Students</Link></li>
                        </AdminWrapper>
                        <AdminWrapper>
                            <li><Link id="class-link" to="classes">Classes</Link> </li>
                        </AdminWrapper>
                        <SuperWrapper>
                            <li><Link id="class-link" to="admin">Site Admin</Link> </li>
                        </SuperWrapper>
                    </ul>
                    <ul className="nav navbar-nav navbar-right">
                        <li><a href="/users/logout">Logout</a></li>
                    </ul>
                </div>
            </div>
        </nav>;
    }
});
