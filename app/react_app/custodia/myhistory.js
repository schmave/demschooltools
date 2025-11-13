const BASE_PATH = "/custodia";

const normalizePath = (path) => {
  if (!path) {
    return BASE_PATH;
  }
  const relative = path.startsWith("/") ? path : `/${path}`;
  if (relative.startsWith(BASE_PATH)) {
    return relative;
  }
  return `${BASE_PATH}${relative}`;
};

const callNavigate = (path, options = {}) => {
  const navigate = window.__custodiaNavigate;
  if (typeof navigate === "function") {
    navigate(normalizePath(path), options);
    return true;
  }
  return false;
};

const push = (path) => {
  if (!callNavigate(path)) {
    window.location.href = normalizePath(path);
  }
};

const replace = (path) => {
  if (!callNavigate(path, { replace: true })) {
    window.location.replace(normalizePath(path));
  }
};

module.exports = { default: { push, replace } };
