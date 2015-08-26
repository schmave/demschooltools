var EventEmitter = require('events').EventEmitter,
    assign = require('object-assign'),
    constants = require('./appconstants'),
    ajax = require('./ajaxhelper');

var isAdmin;
var CHANGE_EVENT = "CHANGE!";

var exports = assign({}, EventEmitter.prototype, {
    isAdmin: function () {
        return isAdmin;
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


module.exports = exports;