import React from "react";
import { EditMinutesPage, ReadMinutesPage, SignInSheetPage } from "../pages";
import { custodiaRoute } from "../custodia/routes";

export const appRoutes = [
  { path: "/editTodayReact", element: <EditMinutesPage /> },
  { path: "/readTodayReact", element: <ReadMinutesPage /> },
  { path: "/attendance/signInSheet", element: <SignInSheetPage /> },
  custodiaRoute,
];
