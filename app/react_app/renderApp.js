import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";

export const mountReactApp = (element) => {
  const root = ReactDOM.createRoot(element);
  root.render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  );
  return root;
};
