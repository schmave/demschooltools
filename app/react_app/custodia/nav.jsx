var React = require("react"),
  Link = require("react-router-dom").Link,
  NavLink = require("react-router-dom").NavLink,
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
          <span className="navbar-brand">
            {this.state.selectedSchool ? this.state.selectedSchool.name : ""} Custodia
          </span>
          <span className="navbar-separator">|</span>
          <span className="custodia-subnav">
            <NavLink to="/custodia/students" className={({ isActive }) => isActive ? "custodia-home-link active" : "custodia-home-link"}>
              Home
            </NavLink>
            <AdminWrapper>
              <NavLink id="totals-link" to="/custodia/reports" className={({ isActive }) => isActive ? "active" : ""}>
                Reports
              </NavLink>
            </AdminWrapper>
            <a href="/custodia/logout">Logout</a>
          </span>
        </div>
      </nav>
    );
  }
};
