import "../index.scss";
import "./polyfill.js";
import "../../../django/static/starter-template.css";
import { mountReactApp } from "../renderApp";

const target =
  document.getElementById("react-root") ||
  document.getElementById("react_container") ||
  document.getElementById("custodia-root");

if (target) {
  mountReactApp(target);
}
