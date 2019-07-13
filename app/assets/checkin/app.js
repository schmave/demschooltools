
const PERSON_KEY_PREFIX = 'person-';
const MESSAGE_KEY_PREFIX = 'message-';

// poll every 2 minutes
const POLLING_INTERVAL_MS = 120000

// after someone checks in/out, automatically return to the home screen after 15 seconds
const WAIT_BEFORE_RESETTING_MS = 15000;

var code_entered;

const container = document.querySelector('#container');
const numpad_template = document.querySelector('#numpad-template');
const authorized_template = document.querySelector('#authorized-template');
const not_authorized_template = document.querySelector('#not-authorized-template');
const overlay = document.querySelector('#overlay');

registerServiceWorker();
resetApp();
poll();

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

function resetApp() {
	container.innerHTML = numpad_template.innerHTML;
	updateCodeEntered('');
	document.querySelectorAll('.number-button').forEach(function(button) {
		button.addEventListener('click', function() {
			updateCodeEntered(code_entered + String(this.dataset.number));
		});
	});
	document.querySelector('.clear-button').addEventListener('click', function() {
		updateCodeEntered('');
	});
	document.querySelector('.arriving-button').addEventListener('click', function() {
		submitCode(true);
	});
	document.querySelector('.leaving-button').addEventListener('click', function() {
		submitCode(false);
	});
	document.querySelectorAll('button').forEach(function(button) {
		button.addEventListener('touchstart', function() {
			this.classList.add('highlight');
		});
		button.addEventListener('touchend', function() {
			this.classList.remove('highlight');
		});
	});
}

function updateCodeEntered(code) {
	code_entered = code;
	hidden_code = '';
	for (let i = 0; i < code_entered.length; i++) {
		hidden_code += '*';
	}
	document.querySelector('#code-entered').innerHTML = hidden_code;
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

async function submitCode(is_arriving) {
	// Setting this class on the overlay prevents any of the buttons from being pressed
	// while we are retrieving person data.
	overlay.classList.add('disabled');
	let person = await getPerson(code_entered);
	overlay.classList.remove('disabled');
	if (person) setAuthorized(person, is_arriving);
	else setUnauthorized();
}

function setAuthorized(person, is_arriving) {
	createMessage(person, is_arriving);
	container.innerHTML = authorized_template.innerHTML;
	document.querySelector('.authorized-text').innerHTML = getAuthorizedText(person, is_arriving);
	// We will automatically return to the home screen after a period of time if the OK
	// button is not pressed.
	var hasReset = false;
	registerOkButtonEvent(function() {
		hasReset = true;
	});
	setTimeout(function() {
		console.log('timeout, hasReset = ' + hasReset);
		if (!hasReset) {
			resetApp();
		}
	}, WAIT_BEFORE_RESETTING_MS);
}

function getAuthorizedText(person, is_arriving) {
	let authorized_text = '';
	if (is_arriving) {
		authorized_text = 'Hello ';
		document.querySelector('.authorized-check').classList.add('hello');
	} else {
		authorized_text = 'Goodbye ';
		document.querySelector('.authorized-check').classList.add('goodbye');
	}
	authorized_text += person.name;
	return authorized_text;
}

function setUnauthorized() {
	container.innerHTML = not_authorized_template.innerHTML;
	registerOkButtonEvent();
}

function registerOkButtonEvent(callback) {
	document.querySelector('.ok-button').addEventListener('click', function() {
		if (callback) callback();
		resetApp();
	});
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
