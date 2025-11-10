const $ = require("jquery");
const dispatcher = require("./appdispatcher");
const constants = require("./appconstants");

function getCookie(name) {
  const cookie = {};
  document.cookie.split(";").forEach(function (el) {
    const split = el.split("=");
    cookie[split[0].trim()] = split.slice(1).join("=");
  });
  return cookie[name];
}

const CSRF_HEADER = {
  "X-CSRFToken": getCookie("csrftoken"),
};

const modifyUrl = (url) => {
  if (url.indexOf("/") === 0) {
    return `/custodia-api${url}`;
  }
  return `/custodia-api/${url}`;
};

const handleError = (xhr) => {
  let message = "Custodia request failed.";
  if (xhr && xhr.status === 0) {
    message = "Unable to reach custodia-api. Make sure the Django server is running.";
  } else if (xhr && xhr.responseJSON && xhr.responseJSON.detail) {
    message = xhr.responseJSON.detail;
  } else if (xhr && xhr.responseText) {
    message = xhr.responseText;
  }
  dispatcher.dispatch({
    type: constants.systemEvents.FLASH,
    message,
    level: "error",
  });
};

const withErrorHandling = (promise) => promise.fail(handleError);

const exports = {
  post: function (url, data) {
    return withErrorHandling(
      $.ajax({
        url: modifyUrl(url),
        method: "POST",
        contentType: "application/json",
        dataType: "json",
        data: JSON.stringify(data),
        headers: CSRF_HEADER,
      }),
    );
  },
  put: function (url, data) {
    return withErrorHandling(
      $.ajax({
        url: modifyUrl(url),
        method: "PUT",
        contentType: "application/json",
        dataType: "json",
        data: JSON.stringify(data),
        headers: CSRF_HEADER,
      }),
    );
  },
  get: function (inObj) {
    const promise =
      typeof inObj === "string"
        ? $.ajax(modifyUrl(inObj))
        : $.ajax(Object.assign({}, inObj, { url: modifyUrl(inObj.url) }));
    return withErrorHandling(promise);
  },
  delete: function (url) {
    return withErrorHandling(
      $.ajax({
        url: modifyUrl(url),
        method: "DELETE",
        headers: CSRF_HEADER,
      }),
    );
  },
};

module.exports = exports;
