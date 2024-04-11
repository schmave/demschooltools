var dispatcher = require("./appdispatcher"),
  base = require("./storebase"),
  constants = require("./appconstants"),
  actionCreator = require("./classactioncreator");

var classes = [];

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
  }
});

module.exports = exports;
