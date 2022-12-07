"use strict";


// This is a compiled version of app.js
// I compiled it using https://babeljs.io/repl#?browsers=chrome%2042
// and then manually added the regenerator runtime




//////////////////////////////////////////
// Regenerator runtime from
//    https://raw.githubusercontent.com/facebook/regenerator/master/packages/regenerator-runtime/runtime.js

/**
 * Copyright (c) 2014-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

var runtime = (function (exports) {
  "use strict";

  var Op = Object.prototype;
  var hasOwn = Op.hasOwnProperty;
  var undefined; // More compressible than void 0.
  var $Symbol = typeof Symbol === "function" ? Symbol : {};
  var iteratorSymbol = $Symbol.iterator || "@@iterator";
  var asyncIteratorSymbol = $Symbol.asyncIterator || "@@asyncIterator";
  var toStringTagSymbol = $Symbol.toStringTag || "@@toStringTag";

  function define(obj, key, value) {
    Object.defineProperty(obj, key, {
      value: value,
      enumerable: true,
      configurable: true,
      writable: true
    });
    return obj[key];
  }
  try {
    // IE 8 has a broken Object.defineProperty that only works on DOM objects.
    define({}, "");
  } catch (err) {
    define = function(obj, key, value) {
      return obj[key] = value;
    };
  }

  function wrap(innerFn, outerFn, self, tryLocsList) {
    // If outerFn provided and outerFn.prototype is a Generator, then outerFn.prototype instanceof Generator.
    var protoGenerator = outerFn && outerFn.prototype instanceof Generator ? outerFn : Generator;
    var generator = Object.create(protoGenerator.prototype);
    var context = new Context(tryLocsList || []);

    // The ._invoke method unifies the implementations of the .next,
    // .throw, and .return methods.
    generator._invoke = makeInvokeMethod(innerFn, self, context);

    return generator;
  }
  exports.wrap = wrap;

  // Try/catch helper to minimize deoptimizations. Returns a completion
  // record like context.tryEntries[i].completion. This interface could
  // have been (and was previously) designed to take a closure to be
  // invoked without arguments, but in all the cases we care about we
  // already have an existing method we want to call, so there's no need
  // to create a new function object. We can even get away with assuming
  // the method takes exactly one argument, since that happens to be true
  // in every case, so we don't have to touch the arguments object. The
  // only additional allocation required is the completion record, which
  // has a stable shape and so hopefully should be cheap to allocate.
  function tryCatch(fn, obj, arg) {
    try {
      return { type: "normal", arg: fn.call(obj, arg) };
    } catch (err) {
      return { type: "throw", arg: err };
    }
  }

  var GenStateSuspendedStart = "suspendedStart";
  var GenStateSuspendedYield = "suspendedYield";
  var GenStateExecuting = "executing";
  var GenStateCompleted = "completed";

  // Returning this object from the innerFn has the same effect as
  // breaking out of the dispatch switch statement.
  var ContinueSentinel = {};

  // Dummy constructor functions that we use as the .constructor and
  // .constructor.prototype properties for functions that return Generator
  // objects. For full spec compliance, you may wish to configure your
  // minifier not to mangle the names of these two functions.
  function Generator() {}
  function GeneratorFunction() {}
  function GeneratorFunctionPrototype() {}

  // This is a polyfill for %IteratorPrototype% for environments that
  // don't natively support it.
  var IteratorPrototype = {};
  IteratorPrototype[iteratorSymbol] = function () {
    return this;
  };

  var getProto = Object.getPrototypeOf;
  var NativeIteratorPrototype = getProto && getProto(getProto(values([])));
  if (NativeIteratorPrototype &&
      NativeIteratorPrototype !== Op &&
      hasOwn.call(NativeIteratorPrototype, iteratorSymbol)) {
    // This environment has a native %IteratorPrototype%; use it instead
    // of the polyfill.
    IteratorPrototype = NativeIteratorPrototype;
  }

  var Gp = GeneratorFunctionPrototype.prototype =
    Generator.prototype = Object.create(IteratorPrototype);
  GeneratorFunction.prototype = Gp.constructor = GeneratorFunctionPrototype;
  GeneratorFunctionPrototype.constructor = GeneratorFunction;
  GeneratorFunction.displayName = define(
    GeneratorFunctionPrototype,
    toStringTagSymbol,
    "GeneratorFunction"
  );

  // Helper for defining the .next, .throw, and .return methods of the
  // Iterator interface in terms of a single ._invoke method.
  function defineIteratorMethods(prototype) {
    ["next", "throw", "return"].forEach(function(method) {
      define(prototype, method, function(arg) {
        return this._invoke(method, arg);
      });
    });
  }

  exports.isGeneratorFunction = function(genFun) {
    var ctor = typeof genFun === "function" && genFun.constructor;
    return ctor
      ? ctor === GeneratorFunction ||
        // For the native GeneratorFunction constructor, the best we can
        // do is to check its .name property.
        (ctor.displayName || ctor.name) === "GeneratorFunction"
      : false;
  };

  exports.mark = function(genFun) {
    if (Object.setPrototypeOf) {
      Object.setPrototypeOf(genFun, GeneratorFunctionPrototype);
    } else {
      genFun.__proto__ = GeneratorFunctionPrototype;
      define(genFun, toStringTagSymbol, "GeneratorFunction");
    }
    genFun.prototype = Object.create(Gp);
    return genFun;
  };

  // Within the body of any async function, `await x` is transformed to
  // `yield regeneratorRuntime.awrap(x)`, so that the runtime can test
  // `hasOwn.call(value, "__await")` to determine if the yielded value is
  // meant to be awaited.
  exports.awrap = function(arg) {
    return { __await: arg };
  };

  function AsyncIterator(generator, PromiseImpl) {
    function invoke(method, arg, resolve, reject) {
      var record = tryCatch(generator[method], generator, arg);
      if (record.type === "throw") {
        reject(record.arg);
      } else {
        var result = record.arg;
        var value = result.value;
        if (value &&
            typeof value === "object" &&
            hasOwn.call(value, "__await")) {
          return PromiseImpl.resolve(value.__await).then(function(value) {
            invoke("next", value, resolve, reject);
          }, function(err) {
            invoke("throw", err, resolve, reject);
          });
        }

        return PromiseImpl.resolve(value).then(function(unwrapped) {
          // When a yielded Promise is resolved, its final value becomes
          // the .value of the Promise<{value,done}> result for the
          // current iteration.
          result.value = unwrapped;
          resolve(result);
        }, function(error) {
          // If a rejected Promise was yielded, throw the rejection back
          // into the async generator function so it can be handled there.
          return invoke("throw", error, resolve, reject);
        });
      }
    }

    var previousPromise;

    function enqueue(method, arg) {
      function callInvokeWithMethodAndArg() {
        return new PromiseImpl(function(resolve, reject) {
          invoke(method, arg, resolve, reject);
        });
      }

      return previousPromise =
        // If enqueue has been called before, then we want to wait until
        // all previous Promises have been resolved before calling invoke,
        // so that results are always delivered in the correct order. If
        // enqueue has not been called before, then it is important to
        // call invoke immediately, without waiting on a callback to fire,
        // so that the async generator function has the opportunity to do
        // any necessary setup in a predictable way. This predictability
        // is why the Promise constructor synchronously invokes its
        // executor callback, and why async functions synchronously
        // execute code before the first await. Since we implement simple
        // async functions in terms of async generators, it is especially
        // important to get this right, even though it requires care.
        previousPromise ? previousPromise.then(
          callInvokeWithMethodAndArg,
          // Avoid propagating failures to Promises returned by later
          // invocations of the iterator.
          callInvokeWithMethodAndArg
        ) : callInvokeWithMethodAndArg();
    }

    // Define the unified helper method that is used to implement .next,
    // .throw, and .return (see defineIteratorMethods).
    this._invoke = enqueue;
  }

  defineIteratorMethods(AsyncIterator.prototype);
  AsyncIterator.prototype[asyncIteratorSymbol] = function () {
    return this;
  };
  exports.AsyncIterator = AsyncIterator;

  // Note that simple async functions are implemented on top of
  // AsyncIterator objects; they just return a Promise for the value of
  // the final result produced by the iterator.
  exports.async = function(innerFn, outerFn, self, tryLocsList, PromiseImpl) {
    if (PromiseImpl === void 0) PromiseImpl = Promise;

    var iter = new AsyncIterator(
      wrap(innerFn, outerFn, self, tryLocsList),
      PromiseImpl
    );

    return exports.isGeneratorFunction(outerFn)
      ? iter // If outerFn is a generator, return the full iterator.
      : iter.next().then(function(result) {
          return result.done ? result.value : iter.next();
        });
  };

  function makeInvokeMethod(innerFn, self, context) {
    var state = GenStateSuspendedStart;

    return function invoke(method, arg) {
      if (state === GenStateExecuting) {
        throw new Error("Generator is already running");
      }

      if (state === GenStateCompleted) {
        if (method === "throw") {
          throw arg;
        }

        // Be forgiving, per 25.3.3.3.3 of the spec:
        // https://people.mozilla.org/~jorendorff/es6-draft.html#sec-generatorresume
        return doneResult();
      }

      context.method = method;
      context.arg = arg;

      while (true) {
        var delegate = context.delegate;
        if (delegate) {
          var delegateResult = maybeInvokeDelegate(delegate, context);
          if (delegateResult) {
            if (delegateResult === ContinueSentinel) continue;
            return delegateResult;
          }
        }

        if (context.method === "next") {
          // Setting context._sent for legacy support of Babel's
          // function.sent implementation.
          context.sent = context._sent = context.arg;

        } else if (context.method === "throw") {
          if (state === GenStateSuspendedStart) {
            state = GenStateCompleted;
            throw context.arg;
          }

          context.dispatchException(context.arg);

        } else if (context.method === "return") {
          context.abrupt("return", context.arg);
        }

        state = GenStateExecuting;

        var record = tryCatch(innerFn, self, context);
        if (record.type === "normal") {
          // If an exception is thrown from innerFn, we leave state ===
          // GenStateExecuting and loop back for another invocation.
          state = context.done
            ? GenStateCompleted
            : GenStateSuspendedYield;

          if (record.arg === ContinueSentinel) {
            continue;
          }

          return {
            value: record.arg,
            done: context.done
          };

        } else if (record.type === "throw") {
          state = GenStateCompleted;
          // Dispatch the exception by looping back around to the
          // context.dispatchException(context.arg) call above.
          context.method = "throw";
          context.arg = record.arg;
        }
      }
    };
  }

  // Call delegate.iterator[context.method](context.arg) and handle the
  // result, either by returning a { value, done } result from the
  // delegate iterator, or by modifying context.method and context.arg,
  // setting context.delegate to null, and returning the ContinueSentinel.
  function maybeInvokeDelegate(delegate, context) {
    var method = delegate.iterator[context.method];
    if (method === undefined) {
      // A .throw or .return when the delegate iterator has no .throw
      // method always terminates the yield* loop.
      context.delegate = null;

      if (context.method === "throw") {
        // Note: ["return"] must be used for ES3 parsing compatibility.
        if (delegate.iterator["return"]) {
          // If the delegate iterator has a return method, give it a
          // chance to clean up.
          context.method = "return";
          context.arg = undefined;
          maybeInvokeDelegate(delegate, context);

          if (context.method === "throw") {
            // If maybeInvokeDelegate(context) changed context.method from
            // "return" to "throw", let that override the TypeError below.
            return ContinueSentinel;
          }
        }

        context.method = "throw";
        context.arg = new TypeError(
          "The iterator does not provide a 'throw' method");
      }

      return ContinueSentinel;
    }

    var record = tryCatch(method, delegate.iterator, context.arg);

    if (record.type === "throw") {
      context.method = "throw";
      context.arg = record.arg;
      context.delegate = null;
      return ContinueSentinel;
    }

    var info = record.arg;

    if (! info) {
      context.method = "throw";
      context.arg = new TypeError("iterator result is not an object");
      context.delegate = null;
      return ContinueSentinel;
    }

    if (info.done) {
      // Assign the result of the finished delegate to the temporary
      // variable specified by delegate.resultName (see delegateYield).
      context[delegate.resultName] = info.value;

      // Resume execution at the desired location (see delegateYield).
      context.next = delegate.nextLoc;

      // If context.method was "throw" but the delegate handled the
      // exception, let the outer generator proceed normally. If
      // context.method was "next", forget context.arg since it has been
      // "consumed" by the delegate iterator. If context.method was
      // "return", allow the original .return call to continue in the
      // outer generator.
      if (context.method !== "return") {
        context.method = "next";
        context.arg = undefined;
      }

    } else {
      // Re-yield the result returned by the delegate method.
      return info;
    }

    // The delegate iterator is finished, so forget it and continue with
    // the outer generator.
    context.delegate = null;
    return ContinueSentinel;
  }

  // Define Generator.prototype.{next,throw,return} in terms of the
  // unified ._invoke helper method.
  defineIteratorMethods(Gp);

  define(Gp, toStringTagSymbol, "Generator");

  // A Generator should always return itself as the iterator object when the
  // @@iterator function is called on it. Some browsers' implementations of the
  // iterator prototype chain incorrectly implement this, causing the Generator
  // object to not be returned from this call. This ensures that doesn't happen.
  // See https://github.com/facebook/regenerator/issues/274 for more details.
  Gp[iteratorSymbol] = function() {
    return this;
  };

  Gp.toString = function() {
    return "[object Generator]";
  };

  function pushTryEntry(locs) {
    var entry = { tryLoc: locs[0] };

    if (1 in locs) {
      entry.catchLoc = locs[1];
    }

    if (2 in locs) {
      entry.finallyLoc = locs[2];
      entry.afterLoc = locs[3];
    }

    this.tryEntries.push(entry);
  }

  function resetTryEntry(entry) {
    var record = entry.completion || {};
    record.type = "normal";
    delete record.arg;
    entry.completion = record;
  }

  function Context(tryLocsList) {
    // The root entry object (effectively a try statement without a catch
    // or a finally block) gives us a place to store values thrown from
    // locations where there is no enclosing try statement.
    this.tryEntries = [{ tryLoc: "root" }];
    tryLocsList.forEach(pushTryEntry, this);
    this.reset(true);
  }

  exports.keys = function(object) {
    var keys = [];
    for (var key in object) {
      keys.push(key);
    }
    keys.reverse();

    // Rather than returning an object with a next method, we keep
    // things simple and return the next function itself.
    return function next() {
      while (keys.length) {
        var key = keys.pop();
        if (key in object) {
          next.value = key;
          next.done = false;
          return next;
        }
      }

      // To avoid creating an additional object, we just hang the .value
      // and .done properties off the next function object itself. This
      // also ensures that the minifier will not anonymize the function.
      next.done = true;
      return next;
    };
  };

  function values(iterable) {
    if (iterable) {
      var iteratorMethod = iterable[iteratorSymbol];
      if (iteratorMethod) {
        return iteratorMethod.call(iterable);
      }

      if (typeof iterable.next === "function") {
        return iterable;
      }

      if (!isNaN(iterable.length)) {
        var i = -1, next = function next() {
          while (++i < iterable.length) {
            if (hasOwn.call(iterable, i)) {
              next.value = iterable[i];
              next.done = false;
              return next;
            }
          }

          next.value = undefined;
          next.done = true;

          return next;
        };

        return next.next = next;
      }
    }

    // Return an iterator with no values.
    return { next: doneResult };
  }
  exports.values = values;

  function doneResult() {
    return { value: undefined, done: true };
  }

  Context.prototype = {
    constructor: Context,

    reset: function(skipTempReset) {
      this.prev = 0;
      this.next = 0;
      // Resetting context._sent for legacy support of Babel's
      // function.sent implementation.
      this.sent = this._sent = undefined;
      this.done = false;
      this.delegate = null;

      this.method = "next";
      this.arg = undefined;

      this.tryEntries.forEach(resetTryEntry);

      if (!skipTempReset) {
        for (var name in this) {
          // Not sure about the optimal order of these conditions:
          if (name.charAt(0) === "t" &&
              hasOwn.call(this, name) &&
              !isNaN(+name.slice(1))) {
            this[name] = undefined;
          }
        }
      }
    },

    stop: function() {
      this.done = true;

      var rootEntry = this.tryEntries[0];
      var rootRecord = rootEntry.completion;
      if (rootRecord.type === "throw") {
        throw rootRecord.arg;
      }

      return this.rval;
    },

    dispatchException: function(exception) {
      if (this.done) {
        throw exception;
      }

      var context = this;
      function handle(loc, caught) {
        record.type = "throw";
        record.arg = exception;
        context.next = loc;

        if (caught) {
          // If the dispatched exception was caught by a catch block,
          // then let that catch block handle the exception normally.
          context.method = "next";
          context.arg = undefined;
        }

        return !! caught;
      }

      for (var i = this.tryEntries.length - 1; i >= 0; --i) {
        var entry = this.tryEntries[i];
        var record = entry.completion;

        if (entry.tryLoc === "root") {
          // Exception thrown outside of any try block that could handle
          // it, so set the completion value of the entire function to
          // throw the exception.
          return handle("end");
        }

        if (entry.tryLoc <= this.prev) {
          var hasCatch = hasOwn.call(entry, "catchLoc");
          var hasFinally = hasOwn.call(entry, "finallyLoc");

          if (hasCatch && hasFinally) {
            if (this.prev < entry.catchLoc) {
              return handle(entry.catchLoc, true);
            } else if (this.prev < entry.finallyLoc) {
              return handle(entry.finallyLoc);
            }

          } else if (hasCatch) {
            if (this.prev < entry.catchLoc) {
              return handle(entry.catchLoc, true);
            }

          } else if (hasFinally) {
            if (this.prev < entry.finallyLoc) {
              return handle(entry.finallyLoc);
            }

          } else {
            throw new Error("try statement without catch or finally");
          }
        }
      }
    },

    abrupt: function(type, arg) {
      for (var i = this.tryEntries.length - 1; i >= 0; --i) {
        var entry = this.tryEntries[i];
        if (entry.tryLoc <= this.prev &&
            hasOwn.call(entry, "finallyLoc") &&
            this.prev < entry.finallyLoc) {
          var finallyEntry = entry;
          break;
        }
      }

      if (finallyEntry &&
          (type === "break" ||
           type === "continue") &&
          finallyEntry.tryLoc <= arg &&
          arg <= finallyEntry.finallyLoc) {
        // Ignore the finally entry if control is not jumping to a
        // location outside the try/catch block.
        finallyEntry = null;
      }

      var record = finallyEntry ? finallyEntry.completion : {};
      record.type = type;
      record.arg = arg;

      if (finallyEntry) {
        this.method = "next";
        this.next = finallyEntry.finallyLoc;
        return ContinueSentinel;
      }

      return this.complete(record);
    },

    complete: function(record, afterLoc) {
      if (record.type === "throw") {
        throw record.arg;
      }

      if (record.type === "break" ||
          record.type === "continue") {
        this.next = record.arg;
      } else if (record.type === "return") {
        this.rval = this.arg = record.arg;
        this.method = "return";
        this.next = "end";
      } else if (record.type === "normal" && afterLoc) {
        this.next = afterLoc;
      }

      return ContinueSentinel;
    },

    finish: function(finallyLoc) {
      for (var i = this.tryEntries.length - 1; i >= 0; --i) {
        var entry = this.tryEntries[i];
        if (entry.finallyLoc === finallyLoc) {
          this.complete(entry.completion, entry.afterLoc);
          resetTryEntry(entry);
          return ContinueSentinel;
        }
      }
    },

    "catch": function(tryLoc) {
      for (var i = this.tryEntries.length - 1; i >= 0; --i) {
        var entry = this.tryEntries[i];
        if (entry.tryLoc === tryLoc) {
          var record = entry.completion;
          if (record.type === "throw") {
            var thrown = record.arg;
            resetTryEntry(entry);
          }
          return thrown;
        }
      }

      // The context.catch method must only be called with a location
      // argument that corresponds to a known catch block.
      throw new Error("illegal catch attempt");
    },

    delegateYield: function(iterable, resultName, nextLoc) {
      this.delegate = {
        iterator: values(iterable),
        resultName: resultName,
        nextLoc: nextLoc
      };

      if (this.method === "next") {
        // Deliberately forget the last sent value so that we don't
        // accidentally pass it on to the delegate.
        this.arg = undefined;
      }

      return ContinueSentinel;
    }
  };

  // Regardless of whether this script is executing as a CommonJS module
  // or not, return the runtime object so that we can declare the variable
  // regeneratorRuntime in the outer scope, which allows this module to be
  // injected easily by `bin/regenerator --include-runtime script.js`.
  return exports;

}(
  // If this script is executing as a CommonJS module, use module.exports
  // as the regeneratorRuntime namespace. Otherwise create a new empty
  // object. Either way, the resulting object will be used to initialize
  // the regeneratorRuntime variable at the top of this file.
  typeof module === "object" ? module.exports : {}
));

try {
  regeneratorRuntime = runtime;
} catch (accidentalStrictMode) {
  // This module should not be running in strict mode, so the above
  // assignment should always work unless something is misconfigured. Just
  // in case runtime.js accidentally runs in strict mode, we can escape
  // strict mode using a global Function call. This could conceivably fail
  // if a Content Security Policy forbids using Function, but in that case
  // the proper solution is to fix the accidental strict mode problem. If
  // you've misconfigured your bundler to force strict mode and applied a
  // CSP to forbid Function, and you're not willing to fix either of those
  // problems, please detail your unique predicament in a GitHub issue.
  Function("r", "regeneratorRuntime = r")(runtime);
}

// end Regenerator runtime
//////////////////////////////



function asyncGeneratorStep(gen, resolve, reject, _next, _throw, key, arg) { try { var info = gen[key](arg); var value = info.value; } catch (error) { reject(error); return; } if (info.done) { resolve(value); } else { Promise.resolve(value).then(_next, _throw); } }

function _asyncToGenerator(fn) { return function () { var self = this, args = arguments; return new Promise(function (resolve, reject) { var gen = fn.apply(self, args); function _next(value) { asyncGeneratorStep(gen, resolve, reject, _next, _throw, "next", value); } function _throw(err) { asyncGeneratorStep(gen, resolve, reject, _next, _throw, "throw", err); } _next(undefined); }); }; }

var LOGIN_INFO_KEY = 'login-info';
var ABSENCE_CODES_KEY = 'absence-codes';
var PERSON_KEY_PREFIX = 'person-';
var MESSAGE_KEY_PREFIX = 'message-';
var ADMIN_MESSAGE_KEY_PREFIX = 'admin-message-'; // poll every 2 minutes

var POLLING_INTERVAL_MS = 120000; // after someone checks in/out, automatically return to the home screen after 15 seconds

var WAIT_BEFORE_RESETTING_MS = 15000;
var code_entered;
var polling_started;
var container = document.querySelector('#container');
var numpad_template = document.querySelector('#numpad-template');
var login_template = document.querySelector('#login-template');
var loading_template = document.querySelector('#loading-template');
var roster_template = document.querySelector('#roster-template');
var roster_failed_template = document.querySelector('#roster-failed-template');
var authorized_template = document.querySelector('#authorized-template');
var not_authorized_template = document.querySelector('#not-authorized-template');
var overlay = document.querySelector('#overlay');
registerServiceWorker();
initializeApp();

function registerServiceWorker() {
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', function () {
      navigator.serviceWorker.register('/assets/checkin/service-worker.js').then(function (reg) {
        console.log('Service worker registered.', reg);
      });
    });
  }
}

function initializeApp() {
  return _initializeApp.apply(this, arguments);
}

function _initializeApp() {
  _initializeApp = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee() {
    var login_info;
    return regeneratorRuntime.wrap(function _callee$(_context) {
      while (1) {
        switch (_context.prev = _context.next) {
          case 0:
            container.innerHTML = loading_template.innerHTML;
            _context.next = 3;
            return getLoginInfo();

          case 3:
            login_info = _context.sent;

            // if this is the first time the app has been loaded on this device,
            // there will be no login info saved, so the user needs to enter it
            if (!login_info) {
              showLoginScreen();
            } else {
              showNumpad();
              polling_started = true;
              poll();
            }

          case 5:
          case "end":
            return _context.stop();
        }
      }
    }, _callee);
  }));
  return _initializeApp.apply(this, arguments);
}

function logIn(_x) {
  return _logIn.apply(this, arguments);
}

function _logIn() {
  _logIn = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee2(login_info) {
    var response, is_login_info_incorrect;
    return regeneratorRuntime.wrap(function _callee2$(_context2) {
      while (1) {
        switch (_context2.prev = _context2.next) {
          case 0:
            console.log('attempting to log in to server');

            if (login_info) {
              _context2.next = 5;
              break;
            }

            _context2.next = 4;
            return getLoginInfo();

          case 4:
            login_info = _context2.sent;

          case 5:
            _context2.next = 7;
            return fetch('/login', {
              body: 'noredirect&email=' + login_info.username + '&password=' + login_info.password,
              headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
              },
              method: 'POST',
              redirect: 'manual'
            });

          case 7:
            response = _context2.sent;

            if (!(response.type === 'opaqueredirect')) {
              _context2.next = 11;
              break;
            }

            // we are now logged in, so tell the caller that they can continue
            // or retry whatever they were trying to do
            console.log('successfully logged in');
            return _context2.abrupt("return", true);

          case 11:
            // The login failed, presumably because the login info was incorrect.
            // The user needs to try entering the login info again.
            is_login_info_incorrect = true;
            showLoginScreen(is_login_info_incorrect);
            return _context2.abrupt("return", false);

          case 14:
          case "end":
            return _context2.stop();
        }
      }
    }, _callee2);
  }));
  return _logIn.apply(this, arguments);
}

function getLoginInfo() {
  return localforage.getItem(LOGIN_INFO_KEY);
}

function saveLoginInfo(login_info) {
  return localforage.setItem(LOGIN_INFO_KEY, login_info).catch(function (err) {
    console.error(err);
  });
}

function showLoginScreen(is_login_info_incorrect) {
  // stop polling while the login screen is showing
  polling_started = false;
  container.innerHTML = login_template.innerHTML;

  if (is_login_info_incorrect) {
    document.querySelector('.login-info-incorrect').hidden = false;
  }

  saveLoginInfo(null);
  document.querySelector('#login-submit').addEventListener('click', function () {
    submitLoginInfo();
  });
}

function submitLoginInfo() {
  return _submitLoginInfo.apply(this, arguments);
}

function _submitLoginInfo() {
  _submitLoginInfo = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee3() {
    var username, password, login_info, success;
    return regeneratorRuntime.wrap(function _callee3$(_context3) {
      while (1) {
        switch (_context3.prev = _context3.next) {
          case 0:
            username = document.querySelector('#username').value;
            password = document.querySelector('#password').value;
            container.innerHTML = loading_template.innerHTML;
            login_info = {
              username: username,
              password: password
            };
            _context3.next = 6;
            return saveLoginInfo(login_info);

          case 6:
            _context3.next = 8;
            return logIn(login_info);

          case 8:
            success = _context3.sent;

            if (!success) {
              _context3.next = 14;
              break;
            }

            _context3.next = 12;
            return downloadData();

          case 12:
            showNumpad();

            if (!polling_started) {
              polling_started = true;
              setTimeout(poll, POLLING_INTERVAL_MS);
            }

          case 14:
          case "end":
            return _context3.stop();
        }
      }
    }, _callee3);
  }));
  return _submitLoginInfo.apply(this, arguments);
}

function showNumpad() {
  container.innerHTML = numpad_template.innerHTML;
  updateCodeEntered('');
  document.querySelectorAll('.number-button').forEach(function (button) {
    button.addEventListener('click', function () {
      updateCodeEntered(code_entered + String(this.dataset.number));
    });
  });
  document.querySelector('.clear-button').addEventListener('click', function () {
    updateCodeEntered('');
  });
  document.querySelector('.roster-button').addEventListener('click', function () {
    showRoster();
  });
  document.querySelector('.arriving-button').addEventListener('click', function () {
    submitCode(true);
  });
  document.querySelector('.leaving-button').addEventListener('click', function () {
    submitCode(false);
  });
  document.querySelectorAll('button').forEach(function (button) {
    button.addEventListener('touchstart', function () {
      this.classList.add('highlight');
    });
    button.addEventListener('touchend', function () {
      this.classList.remove('highlight');
    });
  });
}

function updateCodeEntered(code) {
  code_entered = code;
  var hidden_code = '';

  for (var i = 0; i < code_entered.length; i++) {
    hidden_code += '*';
  }

  document.querySelector('#code-entered').innerHTML = hidden_code;
}

function showRoster(_x2) {
  return _showRoster.apply(this, arguments);
}

function _showRoster() {
  _showRoster = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee4(editable) {
    var person, data, success, people, roster, i, _person, person_row;

    return regeneratorRuntime.wrap(function _callee4$(_context4) {
      while (1) {
        switch (_context4.prev = _context4.next) {
          case 0:
            container.innerHTML = loading_template.innerHTML; // if the admin PIN has been entered, load the roster in editable mode

            _context4.next = 3;
            return getPerson(code_entered);

          case 3:
            person = _context4.sent;

            if (person && person.person_id === -1) {
              editable = true;
            }

            _context4.next = 7;
            return downloadRoster();

          case 7:
            data = _context4.sent;

            if (!(data === -1)) {
              _context4.next = 15;
              break;
            }

            _context4.next = 11;
            return logIn();

          case 11:
            success = _context4.sent;

            if (success) {
              showRoster();
            }

            _context4.next = 38;
            break;

          case 15:
            if (!data) {
              _context4.next = 36;
              break;
            }

            _context4.next = 18;
            return saveAbsenceCodes(data.absence_codes);

          case 18:
            people = data.people;
            container.innerHTML = roster_template.innerHTML;

            if (editable) {
              document.querySelector('#roster-header-title').innerHTML = 'Editable Roster';
            }

            roster = document.getElementById('roster');
            i = 0;

          case 23:
            if (!(i < people.length)) {
              _context4.next = 33;
              break;
            }

            _person = people[i];

            if (!(_person.person_id === -1)) {
              _context4.next = 27;
              break;
            }

            return _context4.abrupt("continue", 30);

          case 27:
            person_row = document.createElement('tr');

            if (editable) {
              buildEditableRosterRow(_person, person_row);
            } else {
              buildRosterRow(_person, person_row);
            }

            roster.appendChild(person_row);

          case 30:
            i++;
            _context4.next = 23;
            break;

          case 33:
            registerCloseButtonEvent();
            _context4.next = 38;
            break;

          case 36:
            container.innerHTML = roster_failed_template.innerHTML;
            registerOkButtonEvent();

          case 38:
          case "end":
            return _context4.stop();
        }
      }
    }, _callee4);
  }));
  return _showRoster.apply(this, arguments);
}

function buildRosterRow(_x3, _x4) {
  return _buildRosterRow.apply(this, arguments);
}

function _buildRosterRow() {
  _buildRosterRow = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee5(person, person_row) {
    var name_column, code_column, in_column, out_column;
    return regeneratorRuntime.wrap(function _callee5$(_context5) {
      while (1) {
        switch (_context5.prev = _context5.next) {
          case 0:
            name_column = document.createElement('td');
            name_column.innerHTML = person.name;
            person_row.appendChild(name_column); // if there is an attendance code, add the code in a 2-span column

            if (person.current_day_code) {
              code_column = document.createElement('td');
              code_column.setAttribute('colspan', 2);
              code_column.className = 'absent';
              code_column.innerHTML = 'Absent';
              person_row.appendChild(code_column);
            } // if there is no attendance code, add in & out columns
            else {
              in_column = document.createElement('td');
              out_column = document.createElement('td');
              in_column.innerHTML = person.current_day_start_time;
              out_column.innerHTML = person.current_day_end_time;
              person_row.appendChild(in_column);
              person_row.appendChild(out_column);
            }

          case 4:
          case "end":
            return _context5.stop();
        }
      }
    }, _callee5);
  }));
  return _buildRosterRow.apply(this, arguments);
}

function buildEditableRosterRow(_x5, _x6) {
  return _buildEditableRosterRow.apply(this, arguments);
}

function _buildEditableRosterRow() {
  _buildEditableRosterRow = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee7(person, person_row) {
    var in_field, out_field, code_field, createNameColumn, createTimeColumn, createAbsenceCodeColumn, _createAbsenceCodeColumn, saveChanges, convertTime;

    return regeneratorRuntime.wrap(function _callee7$(_context7) {
      while (1) {
        switch (_context7.prev = _context7.next) {
          case 0:
            convertTime = function _convertTime(s) {
              if (!s.match(/^[0-9]+$/)) {
                return s;
              }

              if (s.length < 3) {
                s = s + '00';
              }

              var num = parseInt(s);
              var hours = Math.floor(num / 100);
              var minutes = num % 100;

              if (hours < 0 || hours > 12 || minutes < 0 || minutes > 59) {
                return '';
              }

              if (minutes < 10) {
                minutes = '0' + minutes;
              }

              var ampm = 'AM';

              if (hours == 12 || hours <= 6) {
                ampm = 'PM';
              }

              return `${hours}:${minutes} ${ampm}`;
            };

            saveChanges = function _saveChanges() {
              var code = code_field.options[code_field.selectedIndex].value;
              createAdminMessage(person, in_field.value, out_field.value, code);
            };

            _createAbsenceCodeColumn = function _createAbsenceCodeCol2() {
              _createAbsenceCodeColumn = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee6() {
                var column, field, empty_option, absence_codes, i, code, option;
                return regeneratorRuntime.wrap(function _callee6$(_context6) {
                  while (1) {
                    switch (_context6.prev = _context6.next) {
                      case 0:
                        column = document.createElement('td');
                        field = document.createElement('select');
                        empty_option = document.createElement('option');
                        column.className = 'editable';
                        field.appendChild(empty_option);
                        column.appendChild(field);
                        person_row.appendChild(column);
                        _context6.next = 9;
                        return getAbsenceCodes();

                      case 9:
                        absence_codes = _context6.sent;

                        for (i in absence_codes) {
                          code = absence_codes[i];
                          option = document.createElement('option');
                          option.value = option.innerHTML = code;
                          field.appendChild(option);

                          if (person.current_day_code === code) {
                            option.setAttribute('selected', 'selected');
                          }
                        }

                        field.addEventListener('change', function () {
                          in_field.value = '';
                          out_field.value = '';
                          saveChanges();
                        });
                        return _context6.abrupt("return", field);

                      case 13:
                      case "end":
                        return _context6.stop();
                    }
                  }
                }, _callee6);
              }));
              return _createAbsenceCodeColumn.apply(this, arguments);
            };

            createAbsenceCodeColumn = function _createAbsenceCodeCol() {
              return _createAbsenceCodeColumn.apply(this, arguments);
            };

            createTimeColumn = function _createTimeColumn(starting_value) {
              var column = document.createElement('td');
              var field = document.createElement('input');
              column.className = 'editable';
              field.value = starting_value;
              column.appendChild(field);
              person_row.appendChild(column);
              field.addEventListener('click', function () {
                field.select();
              });
              field.addEventListener('change', function () {
                field.value = convertTime(field.value);
                code_field.selectedIndex = 0;
                saveChanges();
              });
              return field;
            };

            createNameColumn = function _createNameColumn() {
              var name_column = document.createElement('td');
              name_column.innerHTML = person.name;
              person_row.appendChild(name_column);
            };

            person_row.innerHTML = '';
            createNameColumn();
            in_field = createTimeColumn(person.current_day_start_time);
            out_field = createTimeColumn(person.current_day_end_time);
            _context7.next = 12;
            return createAbsenceCodeColumn();

          case 12:
            code_field = _context7.sent;

          case 13:
          case "end":
            return _context7.stop();
        }
      }
    }, _callee7);
  }));
  return _buildEditableRosterRow.apply(this, arguments);
}

function poll() {
  return _poll.apply(this, arguments);
}

function _poll() {
  _poll = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee8() {
    return regeneratorRuntime.wrap(function _callee8$(_context8) {
      while (1) {
        switch (_context8.prev = _context8.next) {
          case 0:
            if (!polling_started) {
              _context8.next = 6;
              break;
            }

            console.log('running polling process');
            setTimeout(poll, POLLING_INTERVAL_MS); // wait until downloadData is complete before trying to send messages,
            // because if we are not logged in to the server, downloadData will log
            // us in, but trySendMessages won't

            _context8.next = 5;
            return downloadData();

          case 5:
            trySendMessages();

          case 6:
          case "end":
            return _context8.stop();
        }
      }
    }, _callee8);
  }));
  return _poll.apply(this, arguments);
}

function downloadData() {
  return _downloadData.apply(this, arguments);
}

function _downloadData() {
  _downloadData = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee9() {
    var time, response, success, data, people, pins, i, person;
    return regeneratorRuntime.wrap(function _callee9$(_context9) {
      while (1) {
        switch (_context9.prev = _context9.next) {
          case 0:
            console.log('downloading application data');
            time = new Date().toLocaleString('en-US');
            _context9.next = 4;
            return fetch('/attendance/checkin/data?time=' + time);

          case 4:
            response = _context9.sent;

            if (!response.redirected) {
              _context9.next = 13;
              break;
            }

            _context9.next = 8;
            return logIn();

          case 8:
            success = _context9.sent;

            if (!success) {
              _context9.next = 12;
              break;
            }

            _context9.next = 12;
            return downloadData();

          case 12:
            return _context9.abrupt("return");

          case 13:
            _context9.next = 15;
            return response.json();

          case 15:
            data = _context9.sent;
            people = data.people;
            pins = []; // We don't use the data directly. Instead, we save it in a local db and read it from there.
            // This way, after the data has been downloaded once, the app can work indefinitely without
            // internet connection.

            i = 0;

          case 19:
            if (!(i < people.length)) {
              _context9.next = 29;
              break;
            }

            person = people[i]; // don't save admin if there is no admin code

            if (!(person.person_id === -1 && !person.pin)) {
              _context9.next = 23;
              break;
            }

            return _context9.abrupt("continue", 26);

          case 23:
            pins.push(person.pin);
            _context9.next = 26;
            return savePerson(person);

          case 26:
            i++;
            _context9.next = 19;
            break;

          case 29:
            // clean up old person entries (we don't await this because it's not needed for the app to run)
            localforage.iterate(function (person, key) {
              if (key.startsWith(PERSON_KEY_PREFIX) && !pins.includes(person.pin)) {
                localforage.removeItem(key).catch(function (err) {
                  console.error(err);
                });
              }
            });

          case 30:
          case "end":
            return _context9.stop();
        }
      }
    }, _callee9);
  }));
  return _downloadData.apply(this, arguments);
}

function downloadRoster() {
  return _downloadRoster.apply(this, arguments);
}

function _downloadRoster() {
  _downloadRoster = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee10() {
    var time, response;
    return regeneratorRuntime.wrap(function _callee10$(_context10) {
      while (1) {
        switch (_context10.prev = _context10.next) {
          case 0:
            _context10.prev = 0;
            time = new Date().toLocaleString('en-US');
            _context10.next = 4;
            return fetch('/attendance/checkin/data?time=' + time);

          case 4:
            response = _context10.sent;

            if (!response.redirected) {
              _context10.next = 7;
              break;
            }

            return _context10.abrupt("return", -1);

          case 7:
            if (!(response.status === 200)) {
              _context10.next = 9;
              break;
            }

            return _context10.abrupt("return", response.json());

          case 9:
            _context10.next = 14;
            break;

          case 11:
            _context10.prev = 11;
            _context10.t0 = _context10["catch"](0);
            console.error(_context10.t0);

          case 14:
            return _context10.abrupt("return", null);

          case 15:
          case "end":
            return _context10.stop();
        }
      }
    }, _callee10, null, [[0, 11]]);
  }));
  return _downloadRoster.apply(this, arguments);
}

function getPerson(pin) {
  return localforage.getItem(PERSON_KEY_PREFIX + pin);
}

function savePerson(person) {
  return localforage.setItem(PERSON_KEY_PREFIX + person.pin, person).catch(function (err) {
    console.error(err);
  });
}

function getAbsenceCodes(pin) {
  return localforage.getItem(ABSENCE_CODES_KEY);
}

function saveAbsenceCodes(absence_codes) {
  return localforage.setItem(ABSENCE_CODES_KEY, absence_codes).catch(function (err) {
    console.error(err);
  });
}

function submitCode(_x7) {
  return _submitCode.apply(this, arguments);
}

function _submitCode() {
  _submitCode = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee11(is_arriving) {
    var person;
    return regeneratorRuntime.wrap(function _callee11$(_context11) {
      while (1) {
        switch (_context11.prev = _context11.next) {
          case 0:
            // Setting this class on the overlay prevents any of the buttons from being pressed
            // while we are retrieving person data.
            overlay.classList.add('disabled');
            _context11.next = 3;
            return getPerson(code_entered);

          case 3:
            person = _context11.sent;
            overlay.classList.remove('disabled');

            if (person) {
              // if this is the admin PIN, show the roster in editable mode
              if (person.person_id === -1) {
                showRoster(true);
              } else {
                setAuthorized(person, is_arriving);
              }
            } else {
              setUnauthorized();
            }

          case 6:
          case "end":
            return _context11.stop();
        }
      }
    }, _callee11);
  }));
  return _submitCode.apply(this, arguments);
}

function setAuthorized(person, is_arriving) {
  createMessage(person, is_arriving);
  container.innerHTML = authorized_template.innerHTML;
  document.querySelector('.authorized-text').innerHTML = getAuthorizedText(person, is_arriving);
  if (is_arriving && person.attendance_rate) {
    document.querySelector('.authorized-data').innerHTML = getAuthorizedData(person);
  }
  // We will automatically return to the home screen after a period of time if the OK
  // button is not pressed.

  var hasReset = false;
  registerOkButtonEvent(function () {
    hasReset = true;
  });
  setTimeout(function () {
    console.log('timeout, hasReset = ' + hasReset);

    if (!hasReset) {
      showNumpad();
    }
  }, WAIT_BEFORE_RESETTING_MS);
}

function getAuthorizedText(person, is_arriving) {
  var authorized_text = '';

  if (is_arriving) {
    authorized_text = 'Hello ';
    document.querySelector('.authorized-check').classList.add('hello');
  } else {
    authorized_text = 'Goodbye ';
    document.querySelector('.authorized-check').classList.add('goodbye');
  }

  authorized_text += person.name;
  return authorized_text;
}

function getAuthorizedData(person) {
  return `Your current attendance<br>rate is <strong>${person.attendance_rate}</strong>`;
}

function setUnauthorized() {
  container.innerHTML = not_authorized_template.innerHTML;
  registerOkButtonEvent();
}

function registerOkButtonEvent(callback) {
  document.querySelector('.ok-button').addEventListener('click', function () {
    if (callback) callback();
    showNumpad();
  });
}

function registerCloseButtonEvent() {
  document.querySelector('.close-button').addEventListener('click', function () {
    showNumpad();
  });
}

function createMessage(_x8, _x9) {
  return _createMessage.apply(this, arguments);
}

function _createMessage() {
  _createMessage = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee12(person, is_arriving) {
    var timestamp, roundedTimestamp, message;
    return regeneratorRuntime.wrap(function _callee12$(_context12) {
      while (1) {
        switch (_context12.prev = _context12.next) {
          case 0:
            timestamp = new Date(); // round down to the minute so the actual stored time is consistent with what the user sees

            roundedTimestamp = new Date(timestamp.getFullYear(), timestamp.getMonth(), timestamp.getDate(), timestamp.getHours(), timestamp.getMinutes());
            message = {
              // this is milliseconds elapsed since epoch, which we can use as a unique key
              time: Date.now(),
              // we need this string to be in a specific format so the server can parse it correctly
              time_string: roundedTimestamp.toLocaleString('en-US'),
              person_id: person.person_id,
              is_arriving: is_arriving
            };
            _context12.next = 5;
            return saveMessage(message);

          case 5:
            trySendMessages();

          case 6:
          case "end":
            return _context12.stop();
        }
      }
    }, _callee12);
  }));
  return _createMessage.apply(this, arguments);
}

function createAdminMessage(_x10, _x11, _x12, _x13) {
  return _createAdminMessage.apply(this, arguments);
}

function _createAdminMessage() {
  _createAdminMessage = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee13(person, in_time, out_time, absence_code) {
    var message;
    return regeneratorRuntime.wrap(function _callee13$(_context13) {
      while (1) {
        switch (_context13.prev = _context13.next) {
          case 0:
            message = {
              person_id: person.person_id,
              in_time: in_time,
              out_time: out_time,
              absence_code: absence_code,
              time_string: new Date().toLocaleString('en-US')
            };
            _context13.next = 3;
            return saveAdminMessage(message);

          case 3:
            trySendMessages();

          case 4:
          case "end":
            return _context13.stop();
        }
      }
    }, _callee13);
  }));
  return _createAdminMessage.apply(this, arguments);
}

function saveMessage(_x14) {
  return _saveMessage.apply(this, arguments);
}

function _saveMessage() {
  _saveMessage = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee14(message) {
    return regeneratorRuntime.wrap(function _callee14$(_context14) {
      while (1) {
        switch (_context14.prev = _context14.next) {
          case 0:
            return _context14.abrupt("return", localforage.setItem(MESSAGE_KEY_PREFIX + message.time, message).catch(function (err) {
              console.error(err);
            }));

          case 1:
          case "end":
            return _context14.stop();
        }
      }
    }, _callee14);
  }));
  return _saveMessage.apply(this, arguments);
}

function saveAdminMessage(_x15) {
  return _saveAdminMessage.apply(this, arguments);
}

function _saveAdminMessage() {
  _saveAdminMessage = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee15(message) {
    return regeneratorRuntime.wrap(function _callee15$(_context15) {
      while (1) {
        switch (_context15.prev = _context15.next) {
          case 0:
            return _context15.abrupt("return", localforage.setItem(ADMIN_MESSAGE_KEY_PREFIX + message.person_id, message).catch(function (err) {
              console.error(err);
            }));

          case 1:
          case "end":
            return _context15.stop();
        }
      }
    }, _callee15);
  }));
  return _saveAdminMessage.apply(this, arguments);
}

function trySendMessages() {
  console.log('trying to send queued messages'); // loop through all messages

  localforage.iterate(function (message, key) {
    // Because this function can be called both by a user event and the polling process,
    // it's possible for a second call to begin before the first one ends, which could result
    // in a message being sent twice. This is okay, since the server will ignore or overwrite
    // duplicate messages.
    if (key.startsWith(MESSAGE_KEY_PREFIX) || key.startsWith(ADMIN_MESSAGE_KEY_PREFIX)) {
      // try sending the message
      var query_string, url;

      if (key.startsWith(MESSAGE_KEY_PREFIX)) {
        query_string = `?time_string=${message.time_string}&person_id=${message.person_id}&is_arriving=${message.is_arriving}`;
        url = '/attendance/checkin/message' + query_string;
      } else {
        query_string = `?person_id=${message.person_id}&in_time=${message.in_time}&out_time=${message.out_time}&absence_code=${message.absence_code}&time_string=${message.time_string}`;
        url = '/attendance/checkin/adminmessage' + query_string;
      }

      fetch(url, {
        method: 'POST'
      }).then(function (response) {
        // If the response is good, the server received the message, so we can delete it.
        if (!response.redirected && response.status === 200) {
          localforage.removeItem(key).catch(function (err) {
            console.error(err);
          });
        }
      }).catch(function (err) {
        console.error(err);
      });
    }
  }).catch(function (err) {
    console.error(err);
  });
}