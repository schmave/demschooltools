var React = require('react');

var component = React.createClass({
  displayName: 'FilterBox',
  filterChanged: function(event) {
    this
      .props
      .onFilterChange(event.target.value);
  },
  render: function() {
    return <div style={{padding: "5px"}} className="panel ">
      <form style={{textAlign: "center"}} className="form-inline">
        <input
          style={{width: "90%"}}
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
