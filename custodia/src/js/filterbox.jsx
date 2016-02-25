var React = require('react');

var component = React.createClass({
  filterChanged: function(event) {
    this
      .props
      .onFilterChange(event.target.value);
  },
  render: function() {
    return <div className="panel">
      <form className="form-inline">
        <input
          type="text"
          placeholder="Search..."
          className="form-control"
          ref="filterText"
          onChange={this.filterChanged}/>
      </form>
    </div>;
  }
});

module.exports = component;
