
const PERSON_KEY_PREFIX = 'person-';
const MESSAGE_KEY_PREFIX = 'message-';

// poll every 2 minutes
const POLLING_INTERVAL_MS = 120000

var code_entered;

const container = document.querySelector('#container');
const numpad_template = document.querySelector('#numpad-template');
const authorized_template = document.querySelector('#authorized-template');
const not_authorized_template = document.querySelector('#not-authorized-template');

registerServiceWorker();
initializeApp();

function registerServiceWorker() {
	if ('serviceWorker' in navigator) {
	  	window.addEventListener('load', () => {
		    navigator.serviceWorker.register('/assets/checkin/service-worker.js')
		        .then((reg) => {
		          	console.log('Service worker registered.', reg);
		        });
	  	});
	}
}

function initializeApp() {
	code_entered = '';
	container.innerHTML = numpad_template.innerHTML;
	registerEvents();
	poll();
}

function poll() {
	console.log('Running polling process');
	setTimeout(poll, POLLING_INTERVAL_MS);
	trySendMessages();
	downloadData();
}

async function downloadData() {
	let response = await fetch('/attendance/checkin/data');
	let data = await response.json();
	let pins = [];
	// We don't use the data directly. Instead, we save it in a local db and read it from there.
	// This way, after the data has been downloaded once, the app can work indefinitely without
	// internet connection.
	for (let i = 0; i < data.length; i++) {
		pins.push(data[i].pin);
		savePerson(data[i]);
	}
	// clean up old entries
	localforage.iterate(function(person, key) {
		if (key.startsWith(PERSON_KEY_PREFIX) && !pins.includes(person.pin)) {
			localforage.removeItem(key).catch(function(err) {
			    console.error(err);
			});
		}
	});
}

async function getPerson(pin) {
	return localforage.getItem(PERSON_KEY_PREFIX + pin);
}

async function savePerson(person) {
	return localforage.setItem(PERSON_KEY_PREFIX + person.pin, person).catch(function(err) {
	    console.error(err);
	});
}

function registerEvents() {
	document.querySelectorAll('.number-button').forEach(function(button) {
		button.addEventListener('click', function() {
			addNumberToCode(this.dataset.number);
		});
	});
	document.querySelector('.submit-button').addEventListener('click', function() {
		// TODO separate into two buttons
		submitCode(false);
	});
}

function addNumberToCode(number) {
	code_entered += String(number);
	console.log(code_entered);
}

async function submitCode(is_arriving) {
	// TODO add some kind of visual indicator that the app is waiting (e.g. graying out buttons)
	let person = await getPerson(code_entered);
	if (person) authorized(person, is_arriving);
	else notAuthorized();
}

function notAuthorized() {
	container.innerHTML = not_authorized_template.innerHTML;
	// need a way for user to return to numpad
	// also the numpad should automatically reappear after a minute or so
}

function authorized(person, is_arriving) {
	createMessage(person, is_arriving);
	container.innerHTML = authorized_template.innerHTML;
	let authorized_text = '';
	if (is_arriving) {
		authorized_text = 'Welcome ';
	} else {
		authorized_text = 'Goodbye ';
	}
	authorized_text += person.name;
	document.querySelector('.authorized-text').innerHTML = authorized_text;
}

async function createMessage(person, is_arriving) {
	let message = {
		// this is milliseconds elapsed since epoch, which we can use as a unique key
		time: Date.now(),
		person_id: person.person_id,
		is_arriving: is_arriving
	}
	await saveMessage(message);
	trySendMessages();
}

async function saveMessage(message) {
	return localforage.setItem(MESSAGE_KEY_PREFIX + message.time, message).catch(function(err) {
	    console.error(err);
	});
}

async function trySendMessages() {
	// loop through all messages
	localforage.iterate(function(message, key) {
		// Because this function can be called both by a user event and the polling process,
		// it's possible for a second call to begin before the first one ends, which could result
		// in a message being sent twice. This is okay, since the server will ignore or overwrite
		// duplicate messages.
	    if (key.startsWith(MESSAGE_KEY_PREFIX)) {
	    	// try sending the message
	    	let query_string = `?time=${message.time}&person_id=${message.person_id}&is_arriving=${message.is_arriving}`;
	    	fetch('/attendance/checkin/message' + query_string, { method: 'POST' }).then(response => {
				// If the response is good, the server received the message, so we can delete it.
		        if (response.status === 200) {
		        	localforage.removeItem(key).catch(function(err) {
					    console.error(err);
					});
		        }
            }).catch(function(err) {
			    console.error(err);
			});
	    }
	}).catch(function(err) {
	    console.error(err);
	});
}
