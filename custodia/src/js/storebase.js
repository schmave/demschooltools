const EventEmitter = require("events").EventEmitter;

const CHANGE_EVENT = "change";

const exports = Object.assign({}, EventEmitter.prototype, {
  CHANGE_EVENT,
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
