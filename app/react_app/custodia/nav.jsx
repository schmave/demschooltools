var React = require("react"),
  Link = require("react-router-dom").Link,
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
            <span className="navbar-brand">
              {this.state.selectedSchool ? this.state.selectedSchool.name : ""} Custodia
            </span>
          </div>
          <div id="navbar" className="subnav custodia-subnav">
            <Link to="/custodia/students" className="custodia-home-link">
              &mdash; Home
            </Link>
            <AdminWrapper>
              <Link id="totals-link" to="/custodia/reports">
                Reports
              </Link>
            </AdminWrapper>
            <a href="/custodia/logout">Logout</a>
          </div>
        </div>
      </nav>
    );
  }
};
