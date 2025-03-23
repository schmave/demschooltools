var EventEmitter = require("events").EventEmitter,
  dispatcher = require("./appdispatcher"),
  constants = require("./appconstants");

var CHANGE_EVENT = "change";

var latest = "";
var level = "success";

function getLatest() {
  return { message: latest, level: level };
}

var exports = Object.assign({}, EventEmitter.prototype, {
  getLatest: getLatest,
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

dispatcher.register(function (action) {
  switch (action.type) {
    case constants.systemEvents.FLASH:
      latest = action.message;
      level = action.level || "success";
      exports.emitChange();
      break;
  }
});

module.exports = exports;
