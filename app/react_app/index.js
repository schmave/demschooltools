import './index.scss';
import { mountReactApp } from './renderApp';

const target = document.getElementById('react-root') || document.getElementById('react_container');

if (target) {
  mountReactApp(target);
}
