var exports = {
  post: function (url, data) {
    return $.ajax({
      url: url,
      method: "POST",
      contentType: "application/json",
      dataType: "json",
      data: JSON.stringify(data),
    });
  },
  put: function (url, data) {
    return $.ajax({
      url: url,
      method: "PUT",
      contentType: "application/json",
      dataType: "json",
      data: JSON.stringify(data),
    });
  },
  get: function (url) {
    return $.ajax(url);
  },
  delete: function (url) {
    return $.ajax({
      url: url,
      method: "DELETE",
    });
  },
};

module.exports = exports;
