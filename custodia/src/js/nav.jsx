var React = require("react"),
  Router = require("react-router"),
  Link = Router.Link,
  userStore = require("./userstore"),
  AdminWrapper = require("./adminwrapper.jsx");

module.exports = class Nav extends React.Component {
  state = { selectedSchool: userStore.getSelectedSchool() };

  componentDidMount() {
    userStore.addChangeListener(this._onChange);
  }

  componentWillUnmount() {
    userStore.removeChangeListener(this._onChange);
  }

  _onChange = () => {
    this.setState({ selectedSchool: userStore.getSelectedSchool() });
  };

  render() {
    return (
      <nav className="navbar" role="navigation">
        <div className="container">
          <div className="navbar-header">
            <Link to="/students" id="home" className="navbar-brand">
              {this.state.selectedSchool ? this.state.selectedSchool.name : ""} Custodia &mdash;
              Home
            </Link>
          </div>
          <div id="navbar">
            <ul className="nav navbar-nav">
              <AdminWrapper>
                <li>
                  <Link id="totals-link" to="/reports">
                    Reports
                  </Link>
                </li>
              </AdminWrapper>
            </ul>
            <ul className="nav navbar-nav">
              <li>
                <a href="/custodia/logout">Logout</a>
              </li>
            </ul>
          </div>
        </div>
      </nav>
    );
  }
};
