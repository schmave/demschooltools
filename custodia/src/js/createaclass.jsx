var React = require("react"),
  ReactDOM = require("react-dom"),
  actionCreator = require("./classactioncreator");

class CreateAClass extends React.Component {
  submit = () => {
    actionCreator.createClass(ReactDOM.findDOMNode(this.refs.name).value);
  };

  render() {
    return (
      <div className="row">
        <div className="col-sm-4"></div>
        <div className="col-sm-4">
          <div className="panel panel-success">
            <div className="panel-heading"></div>
            <div className="panel-body">
              <form>
                <div className="form-group">
                  <label htmlFor="Class">Name</label>
                  <input ref="name" className="form-control" id="Class" placeholder="Name" />
                </div>
                <div className="form-group">
                  <button
                    id="create-class-button"
                    type="button"
                    onClick={this.submit}
                    className="btn btn-primary"
                  >
                    Add Class
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

module.exports = CreateAClass;
