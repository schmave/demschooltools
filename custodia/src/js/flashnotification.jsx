import Alert from "@mui/material/Alert";
import Snackbar from "@mui/material/Snackbar";
import { useEffect, useState } from "react";

import { addChangeListener, getLatest, removeChangeListener } from "./flashnotificationstore.js";

function FlashNotification() {
  const [notifications, setNotifications] = useState([]);

  useEffect(() => {
    const handleChange = () => {
      const notification = getLatest();
      if (notification && notification.message) {
        // Add new notification with unique ID
        const newNotification = {
          id: Date.now(),
          message: notification.message,
          severity: notification.level || "success",
        };
        setNotifications((prev) => [...prev, newNotification]);
      }
    };

    addChangeListener(handleChange);

    return () => {
      removeChangeListener(handleChange);
    };
  }, []);

  const handleClose = (id) => (event, reason) => {
    // Don't close on clickaway to ensure user sees the message
    if (reason === "clickaway") {
      return;
    }
    setNotifications((prev) => prev.filter((notification) => notification.id !== id));
  };

  return (
    <>
      {notifications.map((notification, index) => (
        <Snackbar
          key={notification.id}
          open={true}
          autoHideDuration={6000}
          onClose={handleClose(notification.id)}
          anchorOrigin={{ vertical: "top", horizontal: "right" }}
          style={{ top: `${24 + index * 70}px` }}
        >
          <Alert
            onClose={handleClose(notification.id)}
            severity={notification.severity}
            variant="filled"
            sx={{ width: "100%" }}
          >
            {notification.message}
          </Alert>
        </Snackbar>
      ))}
    </>
  );
}

export default FlashNotification;
