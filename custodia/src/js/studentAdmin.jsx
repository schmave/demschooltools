var React = require('react'),
    Router = require('react-router'),
    actionCreator = require('./studentactioncreator'),
    userStore = require('./userstore'),
    Link = Router.Link,
    StudentEditor = require('./student/studentEditor.jsx'),
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

    filterStudents: function(s) {
        return s.filter(function(s){ return s.name.toLocaleLowerCase().indexOf(this.state.filterText.toLocaleLowerCase()) > -1;}.bind(this));
    },

    getActiveStudents: function(){
        var t = this.filterStudents(this.state.students)
                    .map(function (stu) {
                        var link = <Link to={"/students/" + stu._id} id={"student-" + stu._id}>{stu.name}</Link>;
                        return <div key={"t-" + stu._id}  className="col-sm-4">
                          <div>
                            <div className="name"> {link} </div>
                          </div>
                        </div>;
                    }.bind(this));
        return t;
    },

    filterChanged: function(filter){
        this.setState({filterText: filter});
    },

    toggleEdit: function () {
        var edit = !this.state.editing;
        this.setState({editing: edit});
        if (edit) {
            this.refs.studentEditor.edit();
        }
    },

    render: function () {
        var createStudentLink = <span onClick={this.toggleEdit}
                                     className="btn btn-primary btn" id="create-student">
                                 Add Student
                               </span>;
        if(userStore.isDstMode()){
           createStudentLink = <p>Use DemSchoolTools to add students</p>;
        }
        return <div>
                          <StudentEditor ref="studentEditor">
                          </StudentEditor>
                          <div className="row margined class-listing new-class">
                            <div className="col-sm-10 column">
                              <div className="col-sm-2 column">
                                {createStudentLink}
                              </div>
                              <div className="col-sm-10 column">
                                <FilterBox onFilterChange={this.filterChanged} />
                              </div>
                              <div className="col-sm-12 column">
                                <div className="col-sm-12 column">
                                  <div className="panel panel-info">
                                    <div className="panel-heading absent"><b>Students</b></div>
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
