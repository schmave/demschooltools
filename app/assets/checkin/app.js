
const LOGIN_INFO_KEY = 'login-info';
const ABSENCE_CODES_KEY = 'absence-codes';
const PERSON_KEY_PREFIX = 'person-';
const MESSAGE_KEY_PREFIX = 'message-';
const ADMIN_MESSAGE_KEY_PREFIX = 'admin-message-';

// poll every 2 minutes
const POLLING_INTERVAL_MS = 120000

// after someone checks in/out, automatically return to the home screen after 15 seconds
const WAIT_BEFORE_RESETTING_MS = 15000;

var code_entered;
var polling_started;

const container = document.querySelector('#container');
const numpad_template = document.querySelector('#numpad-template');
const login_template = document.querySelector('#login-template');
const loading_template = document.querySelector('#loading-template');
const roster_template = document.querySelector('#roster-template');
const roster_failed_template = document.querySelector('#roster-failed-template');
const authorized_template = document.querySelector('#authorized-template');
const not_authorized_template = document.querySelector('#not-authorized-template');
const overlay = document.querySelector('#overlay');

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

async function initializeApp() {
	container.innerHTML = loading_template.innerHTML;
	let login_info = await getLoginInfo();
	// if this is the first time the app has been loaded on this device,
	// there will be no login info saved, so the user needs to enter it
	if (!login_info) {
		showLoginScreen();
	}
	else {
		showNumpad();
		polling_started = true;
		poll();
	}
}

async function logIn(login_info) {
	console.log('attempting to log in to server');
	if (!login_info) {
		login_info = await getLoginInfo();
	}
	let response = await fetch('/login', {
		body: 'noredirect&email=' + login_info.username + '&password=' + login_info.password,
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
		method: 'POST',
		redirect: 'manual',
	});
	// The server returns a redirect to the home page if the login was successful,
	// and also sets a cookie that will be used for subsequent requests.
	if (response.type === 'opaqueredirect') {
		// we are now logged in, so tell the caller that they can continue
		// or retry whatever they were trying to do
		console.log('successfully logged in');
		return true;
	}
	// The login failed, presumably because the login info was incorrect.
	// The user needs to try entering the login info again.
	let is_login_info_incorrect = true;
	showLoginScreen(is_login_info_incorrect);
	return false;
}

function getLoginInfo() {
	return localforage.getItem(LOGIN_INFO_KEY);
}

function saveLoginInfo(login_info) {
	return localforage.setItem(LOGIN_INFO_KEY, login_info).catch(function(err) {
	    console.error(err);
	});
}

function showLoginScreen(is_login_info_incorrect) {
	// stop polling while the login screen is showing
	polling_started = false;
	container.innerHTML = login_template.innerHTML;
	if (is_login_info_incorrect) {
		document.querySelector('.login-info-incorrect').hidden = false;
	}
	saveLoginInfo(null);
	document.querySelector('#login-submit').addEventListener('click', function() {
		submitLoginInfo();
	});
}

async function submitLoginInfo() {
	let username = document.querySelector('#username').value;
	let password = document.querySelector('#password').value;
	container.innerHTML = loading_template.innerHTML;
	let login_info = { username, password };
	await saveLoginInfo(login_info);
	let success = await logIn(login_info);
	if (success) {
		await downloadData();
		showNumpad();
		if (!polling_started) {
			polling_started = true;
			setTimeout(poll, POLLING_INTERVAL_MS);
		}
	}
}

function showNumpad() {
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
	document.querySelector('.roster-button').addEventListener('click', function() {
		showRoster();
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
	var hidden_code = '';
	for (let i = 0; i < code_entered.length; i++) {
		hidden_code += '*';
	}
	document.querySelector('#code-entered').innerHTML = hidden_code;
}

async function showRoster(editable) {
	container.innerHTML = loading_template.innerHTML;
	// if the admin PIN has been entered, load the roster in editable mode
	let person = await getPerson(code_entered);
	if (person && person.person_id === -1) {
		editable = true;
	}
	let data = await downloadRoster();
	// -1 means we are not logged in to the server
	if (data === -1) {
		let success = await logIn();
		if (success) {
			showRoster();
		}
	}
	else if (data) {
		await saveAbsenceCodes(data.absence_codes);
		let people = data.people;
		container.innerHTML = roster_template.innerHTML;
		if (editable) {
			document.querySelector('#roster-header-title').innerHTML = 'Editable Roster';
		}
		let roster = document.getElementById('roster');
		for (let i = 0; i < people.length; i++) {
			let person = people[i];
			if (person.person_id === -1) continue;
			let person_row = document.createElement('tr');
			if (editable) {
				buildEditableRosterRow(person, person_row);
			} else {
				buildRosterRow(person, person_row);
			}
			roster.appendChild(person_row);
		}
		registerCloseButtonEvent();
	}
	else {
		container.innerHTML = roster_failed_template.innerHTML;
		registerOkButtonEvent();
	}
}

async function buildRosterRow(person, person_row) {
	let name_column = document.createElement('td');
	name_column.innerHTML = person.name;
	person_row.appendChild(name_column);
	// if there is an attendance code, add the code in a 2-span column
	if (person.current_day_code) {
		let code_column = document.createElement('td');
		code_column.setAttribute('colspan', 2);
		code_column.className = 'absent';
		code_column.innerHTML = 'Absent';
		person_row.appendChild(code_column);
	}
	// if there is no attendance code, add in & out columns
	else {
		let in_column = document.createElement('td');
		let out_column = document.createElement('td');
		in_column.innerHTML = person.current_day_start_time;
		out_column.innerHTML = person.current_day_end_time;
		person_row.appendChild(in_column);
		person_row.appendChild(out_column);
	}
}

async function buildEditableRosterRow(person, person_row) {
	person_row.innerHTML = '';
	createNameColumn();
	const in_field = createTimeColumn(person.current_day_start_time);
	const out_field = createTimeColumn(person.current_day_end_time);
	const code_field = await createAbsenceCodeColumn();

	function createNameColumn() {
		let name_column = document.createElement('td');
		name_column.innerHTML = person.name;
		person_row.appendChild(name_column);
	}

	function createTimeColumn(starting_value) {
		let column = document.createElement('td');
		let field = document.createElement('input');
		column.className = 'editable';
		field.value = starting_value;
		column.appendChild(field);
		person_row.appendChild(column);
		field.addEventListener('click', function() {
			field.select();
		});
		field.addEventListener('change', function() {
			field.value = convertTime(field.value);
			code_field.selectedIndex = 0;
			saveChanges();
		});
		return field;
	}

	async function createAbsenceCodeColumn() {
		let column = document.createElement('td');
		let field = document.createElement('select');
		let empty_option = document.createElement('option');
		column.className = 'editable';
		field.appendChild(empty_option);
		column.appendChild(field);
		person_row.appendChild(column);

		let absence_codes = await getAbsenceCodes();
		for (let i in absence_codes) {
			let code = absence_codes[i];
			let option = document.createElement('option');
			option.value = option.innerHTML = code;
			field.appendChild(option);
			if (person.current_day_code === code) {
				option.setAttribute('selected', 'selected');
			}
		}
		field.addEventListener('change', function() {
			in_field.value = '';
			out_field.value = '';
			saveChanges();
		});
		return field;
	}

	function saveChanges() {
		let code = code_field.options[code_field.selectedIndex].value;
		createAdminMessage(person, in_field.value, out_field.value, code);
	}

	function convertTime(s) {
	    if (!s.match(/^[0-9]+$/)) {
	        return s;
	    }
	    if (s.length < 3) {
	        s = s + '00';
	    }
	    let num = parseInt(s);
	    let hours = Math.floor(num / 100);
	    let minutes = num % 100;
	    if (hours < 0 || hours > 12 || minutes < 0 || minutes > 59) {
	        return '';
	    }
	    if (minutes < 10) {
	        minutes = '0' + minutes;
	    }
	    var ampm = 'AM';
	    if (hours == 12 || hours <= 6) {
	        ampm = 'PM';
	    }
	    return `${hours}:${minutes} ${ampm}`;
	}
}

async function poll() {
	if (polling_started) {
		console.log('running polling process');
		setTimeout(poll, POLLING_INTERVAL_MS);
		// wait until downloadData is complete before trying to send messages,
		// because if we are not logged in to the server, downloadData will log
		// us in, but trySendMessages won't
		await downloadData();
		trySendMessages();
	}
}

async function downloadData() {
	console.log('downloading application data');
	let time = new Date().toLocaleString('en-US');
	let response = await fetch('/attendance/checkin/data?time=' + time);
	// 'redirected' means we are not logged in to the server
	if (response.redirected) {
		let success = await logIn();
		if (success) {
			// we are now logged in, try again
			await downloadData();
		}
		return;
	}
	let data = await response.json();
	let people = data.people;
	let pins = [];
	// We don't use the data directly. Instead, we save it in a local db and read it from there.
	// This way, after the data has been downloaded once, the app can work indefinitely without
	// internet connection.
	for (let i = 0; i < people.length; i++) {
		let person = people[i];
		// don't save admin if there is no admin code
		if (person.person_id === -1 && !person.pin) continue;
		pins.push(person.pin);
		await savePerson(person);
	}
	// clean up old person entries (we don't await this because it's not needed for the app to run)
	localforage.iterate(function(person, key) {
		if (key.startsWith(PERSON_KEY_PREFIX) && !pins.includes(person.pin)) {
			localforage.removeItem(key).catch(function(err) {
			    console.error(err);
			});
		}
	});
}

async function downloadRoster() {
	// The "roster" data is the same as the application data; the difference is that when the
	// user requests the roster, we have to give them up-to-date data from the server, or
	// nothing. This allows the user to see data that is guaranteed to be current, or allows
	// them to see that the app is offline.
	try {
		let time = new Date().toLocaleString('en-US');
		let response = await fetch('/attendance/checkin/data?time=' + time);
		// 'redirected' means we are not logged in to the server
		if (response.redirected) {
			return -1;
		}
	    if (response.status === 200) {
	    	return response.json();
	    }
	} catch (err) {
		console.error(err);
	}
    return null;
}

function getPerson(pin) {
	return localforage.getItem(PERSON_KEY_PREFIX + pin);
}

function savePerson(person) {
	return localforage.setItem(PERSON_KEY_PREFIX + person.pin, person).catch(function(err) {
	    console.error(err);
	});
}

function getAbsenceCodes(pin) {
	return localforage.getItem(ABSENCE_CODES_KEY);
}

function saveAbsenceCodes(absence_codes) {
	return localforage.setItem(ABSENCE_CODES_KEY, absence_codes).catch(function(err) {
	    console.error(err);
	});
}

async function submitCode(is_arriving) {
	// Setting this class on the overlay prevents any of the buttons from being pressed
	// while we are retrieving person data.
	overlay.classList.add('disabled');
	let person = await getPerson(code_entered);
	overlay.classList.remove('disabled');
	if (person) {
		// if this is the admin PIN, show the roster in editable mode
		if (person.person_id === -1) {
			showRoster(true);
		} else {
			setAuthorized(person, is_arriving);
		}
	} else {
		setUnauthorized();
	}
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
			showNumpad();
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
		showNumpad()
	});
}

function registerCloseButtonEvent() {
	document.querySelector('.close-button').addEventListener('click', function() {
		showNumpad();
	});
}

async function createMessage(person, is_arriving) {
	let message = {
		// this is milliseconds elapsed since epoch, which we can use as a unique key
		time: Date.now(),
		// we need this string to be in a specific format so the server can parse it correctly
		time_string: new Date().toLocaleString('en-US'),
		person_id: person.person_id,
		is_arriving: is_arriving
	}
	await saveMessage(message);
	trySendMessages();
}

async function createAdminMessage(person, in_time, out_time, absence_code) {
	let message = {
		person_id: person.person_id,
		in_time: in_time,
		out_time: out_time,
		absence_code: absence_code,
		time_string: new Date().toLocaleString('en-US')
	}
	await saveAdminMessage(message);
	trySendMessages();
}

async function saveMessage(message) {
	return localforage.setItem(MESSAGE_KEY_PREFIX + message.time, message).catch(function(err) {
	    console.error(err);
	});
}

async function saveAdminMessage(message) {
	// Use person_id as the unique key for messages, that way if the same person's data is edited
	// multiple times, later edits will overwrite earlier edits.
	return localforage.setItem(ADMIN_MESSAGE_KEY_PREFIX + message.person_id, message).catch(function(err) {
	    console.error(err);
	});
}

function trySendMessages() {
	console.log('trying to send queued messages');
	// loop through all messages
	localforage.iterate(function(message, key) {
		// Because this function can be called both by a user event and the polling process,
		// it's possible for a second call to begin before the first one ends, which could result
		// in a message being sent twice. This is okay, since the server will ignore or overwrite
		// duplicate messages.
	    if (key.startsWith(MESSAGE_KEY_PREFIX) || key.startsWith(ADMIN_MESSAGE_KEY_PREFIX)) {
	    	// try sending the message
	    	let query_string, url;
	    	if (key.startsWith(MESSAGE_KEY_PREFIX)) {
	    		query_string = `?time_string=${message.time_string}&person_id=${message.person_id}&is_arriving=${message.is_arriving}`;
	    		url = '/attendance/checkin/message' + query_string;
	    	} else {
	    		query_string = `?person_id=${message.person_id}&in_time=${message.in_time}&out_time=${message.out_time}&absence_code=${message.absence_code}&time_string=${message.time_string}`;
	    		url = '/attendance/checkin/adminmessage' + query_string;
	    	}
	    	fetch(url, { method: 'POST' }).then(response => {
				// If the response is good, the server received the message, so we can delete it.
		        if (!response.redirected && response.status === 200) {
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
