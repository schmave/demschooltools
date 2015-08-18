(function () {
    if (!Array.prototype.find) {
        Array.prototype.find = function (predicate) {
            if (this == null) {
                throw new TypeError('Array.prototype.findIndex called on null or undefined');
            }
            if (typeof predicate !== 'function') {
                throw new TypeError('predicate must be a function');
            }
            var list = Object(this);
            var length = list.length >>> 0;
            var thisArg = arguments[1];
            var value;

            for (var i = 0; i < length; i++) {
                value = list[i];
                if (predicate.call(thisArg, value, i, list)) {
                    return this[i];
                }
            }
            return undefined;
        };
    }

    if (!String.prototype.startsWith) {
        String.prototype.startsWith = function (searchString, position) {
            position = position || 0;
            return this.indexOf(searchString, position) === position;
        };
    }

    Array.prototype.partition = function (size) {
        var output = [];

        for (var i = 0; i < this.length; i += size) {
            output[output.length] = this.slice(i, i + size);
        }

        return output;
    };

    var add = function (map, key, val) {
        if (!map.hasOwnProperty(key)) {
            map[key] = [];
        }
        map[key].push(val);
    };

    Array.prototype.groupBy = function (predicate) {
        return this.reduce(function (acc, current) {
            add(acc, predicate(current), current);
            return acc;
        }, {});
    }
})();
