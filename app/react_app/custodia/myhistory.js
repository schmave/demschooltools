const callNavigate = (path, options = {}) => {
  const navigate = window.__custodiaNavigate;
  if (typeof navigate === "function") {
    navigate(path, options);
    return true;
  }
  return false;
};

const push = (path) => {
  if (!callNavigate(path)) {
    const normalized = path.startsWith("/") ? path : `/${path}`;
    window.location.href = normalized;
  }
};

const replace = (path) => {
  if (!callNavigate(path, { replace: true })) {
    const normalized = path.startsWith("/") ? path : `/${path}`;
    window.location.replace(normalized);
  }
};

module.exports = { default: { push, replace } };
