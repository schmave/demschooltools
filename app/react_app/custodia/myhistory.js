const normalizePath = (path) => {
  if (!path) {
    return "/";
  }
  return path.startsWith("/") ? path : `/${path}`;
};

const push = (path) => {
  const normalized = normalizePath(path);
  const targetHash = `#${normalized}`;
  if (window.location.hash === targetHash) {
    return;
  }
  window.location.hash = targetHash;
};

const dispatchHashChange = () => {
  try {
    window.dispatchEvent(new HashChangeEvent("hashchange"));
  } catch (e) {
    const event = document.createEvent("HTMLEvents");
    event.initEvent("hashchange", true, true);
    window.dispatchEvent(event);
  }
};

const replace = (path) => {
  const normalized = normalizePath(path);
  const base = `${window.location.origin}${window.location.pathname}${window.location.search}`;
  window.history.replaceState(null, "", `${base}#${normalized}`);
  dispatchHashChange();
};

const history = {
  push,
  replace,
};

module.exports = { default: history };
