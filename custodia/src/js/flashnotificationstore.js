var EventEmitter = require('events').EventEmitter,
    dispatcher = require('./appdispatcher'),
    assign = require('object-assign'),
    constants = require('./appconstants');

var CHANGE_EVENT = 'change';

var latest = '';

function getLatest(){
    return latest;
}

var exports = assign({}, EventEmitter.prototype, {
    getLatest: getLatest,
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

dispatcher.register(function(action){
    switch(action.type){
        case constants.systemEvents.FLASH:
            latest = action.message;
            exports.emitChange();
            break;
    }
});

module.exports = exports;
