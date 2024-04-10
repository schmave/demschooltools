var EventEmitter = require("events").EventEmitter,
  dispatcher = require("./appdispatcher"),
  base = require("./storebase"),
  constants = require("./appconstants"),
  actionCreator = require("./classactioncreator");

var isAdmin;
var classes = [];
var schoolYears;

var exports = Object.create(base);

exports.getClasses = function (force) {
  if (classes.length == 0 || force) {
    actionCreator.loadClasses();
    return [];
  } else {
    return classes;
  }
};

dispatcher.register(function (action) {
  switch (action.type) {
    case constants.classEvents.CLASSES_LOADED:
      classes = action.data;
      exports.emitChange();
      break;
    case constants.classEvents.CLASS_CREATED:
      classes = action.data;
      exports.emitChange();
      break;
    case constants.classEvents.CLASS_STUDENT_ADDED:
      classes = action.data;
      exports.emitChange();
      break;
    case constants.classEvents.CLASS_CHANGED:
      classes = action.data;
      exports.emitChange();
      break;
    case constants.classEvents.CLASS_STUDENT_DELETED:
      classes = action.data;
      exports.emitChange();
      break;
  }
});

module.exports = exports;
