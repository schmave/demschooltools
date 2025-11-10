import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";

const normalizePath = (path) => {
  if (!path) {
    return "/";
  }
  return path.startsWith("/") ? path : `/${path}`;
};

const NavigationBridge = ({ children }) => {
  const navigate = useNavigate();
  const navigateRef = useRef();

  useEffect(() => {
    navigateRef.current = (path, options = {}) => {
      navigate(normalizePath(path), options);
    };
    window.__custodiaNavigate = navigateRef.current;
    return () => {
      if (window.__custodiaNavigate === navigateRef.current) {
        delete window.__custodiaNavigate;
      }
    };
  }, [navigate]);

  return children;
};

export default NavigationBridge;
