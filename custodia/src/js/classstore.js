var EventEmitter = require('events').EventEmitter,
    dispatcher = require('./appdispatcher'),
    base = require('./storebase'),
    constants = require('./appconstants'),
    actionCreator = require('./reportactioncreator');

var isAdmin;
var classes = {};
var schoolYears;

var exports = Object.create(base);

exports.getClasses = function (force) {
    if (!classes || force) {
        actionCreator.loadClasses();
    } else {
        return classes;
    }
};

exports.removeChangeListener = function (callback) {
    this.removeListener(this.CHANGE_EVENT, callback);
    if (this.listeners(this.CHANGE_EVENT).length == 0) {
        classes = {};
    }
};

dispatcher.register(function (action) {
    switch (action.type) {
        case constants.classEvents.CLASSES_LOADED:
            classes[action.data.year] = action.data.report;
            exports.emitChange();
            break;
        case constants.classEvents.CLASS_CREATED:
            exports.getSchoolYears(true);
            break;
        case constants.classEvents.CLASS_DELETED:
            schoolYears = action.data;
            exports.emitChange();
            break;
    }
});

module.exports = exports;
