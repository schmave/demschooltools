import { EventEmitter } from "events";

const exports = Object.assign({}, EventEmitter.prototype, {
  emitChange: function () {
    this.emit("change");
  },

  addChangeListener: function (callback) {
    this.on("change", callback);
  },

  removeChangeListener: function (callback) {
    this.removeListener("change", callback);
  },
});

export default exports;
