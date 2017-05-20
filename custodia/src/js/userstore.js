var EventEmitter = require('events').EventEmitter,
    assign = require('object-assign'),
    constants = require('./appconstants'),
    ajax = require('./ajaxhelper');

var isAdmin;
var isSuper;
var users =[];
var schemas = [];
var superSchool;
var CHANGE_EVENT = "CHANGE!";

var exports = assign({}, EventEmitter.prototype, {
    isAdmin: function () {
        return isAdmin;
    },
    isSuper: function () {
        return isSuper;
    },
    getUsers: function () {
        return users;
    },
    getSchools: function () {
        return schemas;
    },
    getSuperSchema: function() {
        return superSchool;
    },
    setSuperSchema: function(school) {
        var route = 'school/' + school._id;
        ajax.put(route).then(function (data) {
            superSchool = school;
            exports.emitChange();
        }.bind(this));
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
    isAdmin = data.admin;
    exports.emitChange();
}, function (data) {
    isAdmin = false;
    exports.emitChange();
});

ajax.get('/users/is-super').then(function (data) {
    isSuper = data.super;
    superSchool = data.schema;
    exports.emitChange();
    if (data.super) {
        ajax.get('/user').then(function (data) {
            users = data.users;
            exports.emitChange();
        }, function (data) {});

        ajax.get('/schools').then(function (data) {
            schemas = data;
            exports.emitChange();
        }, function (data) {
            exports.emitChange();
        });
    }
}, function (data) {
    isSuper = false;
    exports.emitChange();
});


module.exports = exports;
