var React = require('react'),
    Router = require('react-router'),
    actionCreator = require('./studentactioncreator'),
    Link = Router.Link,
    AdminWrapper = require('./adminwrapper.jsx'),
    studentStore = require('./StudentStore'),
    FilterBox = require('./filterbox.jsx');

var exports = React.createClass({
    getInitialState: function () {
        return {
            filterText: '',
            students: studentStore.getAllStudents(true)
        };
    },

    setupState: function (students) {
        this.setState({
            students: students,
        });
    },

    componentDidMount: function () {
        studentStore.addChangeListener(this._onStudentChange);
    },

    componentWillUnmount: function () {
        studentStore.removeChangeListener(this._onStudentChange);
    },

    _onStudentChange: function () {
        this.setupState(studentStore.getAllStudents());
    },

    getActiveStudents : function(){
        var t = this.state.students
                    .map(function (stu) { return <div key={"t-" + stu._id}  className="in-class panel panel-info student-listing col-sm-4">
                      <div>
                        <div className="name"> {stu.name} </div>
                      </div>
                    </div>;
                    }.bind(this));
        return t;
    },

    filterChanged: function(filter){
        this.setState({filterText: filter});
    },

    render: function () {
        return <div>
                          <div className="row margined class-listing new-class">
                            <div className="col-sm-10 column">
                              <div className="col-sm-2 column">
                                <Link to="create" className="btn btn-primary btn-xs" id="create-student">
                                  Add Student
                                </Link>
                              </div>
                              <div className="col-sm-10 column">
                                <FilterBox onFilterChange={this.filterChanged} />
                              </div>
                              <div className="col-sm-12 column">
                                <div className="col-sm-12 column">
                                  <div className="panel panel-info">
                                    <div className="panel-heading absent"><b>Active</b></div>
                                    {this.getActiveStudents()}
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>
        </div>;
    }
});

module.exports = exports;
