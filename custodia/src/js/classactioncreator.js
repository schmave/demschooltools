var constants = require("./appconstants"),
  ajax = require("./ajaxhelper"),
  dispatcher = require("./appdispatcher");

var exports = {
  loadClasses: function () {
    ajax
      .get({
        url: "/classes",
      })
      .then(function (data) {
        dispatcher.dispatch({
          type: constants.classEvents.CLASSES_LOADED,
          data: data,
        });
      });
  },
};

module.exports = exports;
