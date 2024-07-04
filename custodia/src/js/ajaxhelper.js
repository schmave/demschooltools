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

const exports = {
  post: function (url, data) {
    return $.ajax({
      url,
      method: "POST",
      contentType: "application/json",
      dataType: "json",
      data: JSON.stringify(data),
      headers: CSRF_HEADER,
    });
  },
  put: function (url, data) {
    return $.ajax({
      url,
      method: "PUT",
      contentType: "application/json",
      dataType: "json",
      data: JSON.stringify(data),
      headers: CSRF_HEADER,
    });
  },
  get: function (url) {
    return $.ajax(url);
  },
  delete: function (url) {
    return $.ajax({
      url,
      method: "DELETE",
      headers: CSRF_HEADER,
    });
  },
};

module.exports = exports;
