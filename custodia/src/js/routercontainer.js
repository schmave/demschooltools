var _router = null;

module.exports = {
    set: function(router){
        _router = router;
    },
    get: function(){
        return _router;
    }
}
