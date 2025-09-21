const $ = require("jquery");

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

const exports = {
  post: function (url, data) {
    return $.ajax({
      url: modifyUrl(url),
      method: "POST",
      contentType: "application/json",
      dataType: "json",
      data: JSON.stringify(data),
      headers: CSRF_HEADER,
    });
  },
  put: function (url, data) {
    return $.ajax({
      url: modifyUrl(url),
      method: "PUT",
      contentType: "application/json",
      dataType: "json",
      data: JSON.stringify(data),
      headers: CSRF_HEADER,
    });
  },
  get: function (inObj) {
    if (typeof inObj === "string") {
      return $.ajax(modifyUrl(inObj));
    }
    return $.ajax(Object.assign({}, inObj, { url: modifyUrl(inObj.url) }));
  },
  delete: function (url) {
    return $.ajax({
      url: modifyUrl(url),
      method: "DELETE",
      headers: CSRF_HEADER,
    });
  },
};

module.exports = exports;
