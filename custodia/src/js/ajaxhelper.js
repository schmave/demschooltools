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

// Helper function to handle fetch responses
const handleResponse = async (response) => {
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    return response.json();
  }
  return response.text();
};

const exports = {
  post: function (url, data) {
    return fetch(modifyUrl(url), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...CSRF_HEADER,
      },
      body: JSON.stringify(data),
    }).then(handleResponse);
  },
  put: function (url, data) {
    return fetch(modifyUrl(url), {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        ...CSRF_HEADER,
      },
      body: JSON.stringify(data),
    }).then(handleResponse);
  },
  get: function (inObj) {
    if (typeof inObj === "string") {
      return fetch(modifyUrl(inObj)).then(handleResponse);
    }
    const { url, ...options } = inObj;
    return fetch(modifyUrl(url), options).then(handleResponse);
  },
  delete: function (url) {
    return fetch(modifyUrl(url), {
      method: "DELETE",
      headers: CSRF_HEADER,
    }).then(handleResponse);
  },
};

export default exports;
