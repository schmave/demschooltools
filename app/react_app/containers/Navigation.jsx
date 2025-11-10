import React, { useMemo } from "react";
import { RouterProvider, createBrowserRouter } from "react-router-dom";
import { appRoutes } from "../routes";

const Navigation = () => {
  const router = useMemo(() => createBrowserRouter(appRoutes), []);
  return <RouterProvider router={router} />;
};

export default Navigation;
