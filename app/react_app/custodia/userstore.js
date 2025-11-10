var EventEmitter = require("events").EventEmitter,
  ajax = require("./ajaxhelper");

var isAdmin;
var users = [];
var schools = [];
var currentSchool;
var CHANGE_EVENT = "CHANGE!";

var exports = Object.assign({}, EventEmitter.prototype, {
  isAdmin: function () {
    return isAdmin;
  },
  getUsers: function () {
    return users;
  },
  getSchools: function () {
    return schools;
  },
  getSelectedSchool: function () {
    return currentSchool;
  },
  emitChange: function () {
    this.emit(CHANGE_EVENT);
  },
  addChangeListener: function (callback) {
    this.on(CHANGE_EVENT, callback);
  },
  removeChangeListener: function (callback) {
    this.removeListener(CHANGE_EVENT, callback);
  },
});

ajax.get("/users/is-admin").then(
  function (data) {
    isAdmin = data.admin;
    currentSchool = data.school;
    exports.emitChange();
  },
  function () {
    isAdmin = false;
    exports.emitChange();
  },
);

module.exports = exports;
