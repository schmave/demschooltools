var EventEmitter = require('events').EventEmitter,
    assign = require('object-assign'),
    constants = require('./appconstants'),
    ajax = require('./ajaxhelper');

var isAdmin;
var isSuper;
var schemas = [];
var CHANGE_EVENT = "CHANGE!";

var exports = assign({}, EventEmitter.prototype, {
    isAdmin: function () {
        return isAdmin;
    },
    isSuper: function () {
        return isSuper;
    },
    getSchemas: function () {
        return schemas;
    },
    emitChange: function(){
        this.emit(CHANGE_EVENT);
    },
    addChangeListener: function(callback){
        this.on(CHANGE_EVENT, callback);
    },
    removeChangeListener: function(callback){
        this.removeListener(CHANGE_EVENT, callback);
    }
});

ajax.get('/users/is-admin').then(function (data) {
    isAdmin = true;
    exports.emitChange();
}, function (data) {
    isAdmin = false;
    exports.emitChange();
});

ajax.get('/users/is-super').then(function (data) {
    isSuper = true;
    exports.emitChange();
}, function (data) {
    isSuper = false;
    exports.emitChange();
});

ajax.get('/schemas').then(function (data) {
    schemas = data;
    exports.emitChange();
}, function (data) {
    exports.emitChange();
});


module.exports = exports;
