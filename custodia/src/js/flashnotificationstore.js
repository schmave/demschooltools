import { EventEmitter } from "events";

import constants from "./appconstants.js";
import dispatcher from "./appdispatcher.js";

const emitter = new EventEmitter();

const CHANGE_EVENT = "change";

let latest = "";
let level = "success";

export const getLatest = ()=> {
  return { message: latest, level: level };
};

export const emitChange = () => {
  console.log('emitChange', latest, level);
  emitter.emit(CHANGE_EVENT);
};

export const addChangeListener = (callback)=> {
  emitter.on(CHANGE_EVENT, callback);
};

export const removeChangeListener = (callback)=>  {
  emitter.removeListener(CHANGE_EVENT, callback);
};

dispatcher.register(function (action) {
  switch (action.type) {
    case constants.systemEvents.FLASH:
      latest = action.message;
      level = action.level || "success";
      emitChange();
      break;
  }
});
