var EventEmitter = require("events").EventEmitter,
  assign = require("object-assign");

var CHANGE_EVENT = "change";

var exports = assign({}, EventEmitter.prototype, {
  CHANGE_EVENT: CHANGE_EVENT,
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

module.exports = exports;
