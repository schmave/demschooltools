
var code_entered;
var ready = false;

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

async function initializeApp() {
	code_entered = '';
	container.innerHTML = numpad_template.innerHTML;
	registerEvents();
	// The user can begin using the numpad even before initial data is downloaded.
	// If they submit their PIN before data has finished downloading, then they will have to wait.
	let data = await downloadData();
	// We don't use the data directly. Instead, we save it in a local db and read it from there.
	// This way, after the data has been downloaded once, the app can work indefinitely without
	// internet connection.
	await saveData(data);
	ready = true;
}

async function downloadData() {
	let response = await fetch('/attendance/checkin/data');
	return await response.json();
}

async function saveData(data) {
	console.log(data);
	for (let i = 0; i < data.length; i++) {
		let person = data[i];
		await localforage.setItem(person.pin, person).catch(function(err) {
		    console.error(err);
		});
	}
}

async function getPerson(pin) {
	return await localforage.getItem(pin);
}

function registerEvents() {
	document.querySelectorAll('.number-button').forEach(function(button) {
		button.addEventListener('click', function() {
			addNumberToCodeEntered(this.dataset.number);
		});
	});
	document.querySelector('.submit-button').addEventListener('click', submitCode);
}

function addNumberToCodeEntered(number) {
	code_entered += String(number);
	console.log(code_entered);
}

function submitCode() {
	getPerson(code_entered).then(function(person) {
		if (person) authorized(person);
		else notAuthorized();
	});
}

function authorized(person) {
	container.innerHTML = authorized_template.innerHTML;
	let authorized_text = '';
	// person is considered arriving if this is the first time they've done this today.
	// otherwise they are considered departing.
	const is_arriving = person.records_today === 0;
	// need to save new person data in local DB
	person.records_today++;
	// need to send server the person ID and current time.
	// this will involve creating a message object in local DB which will sync to
	// server whenever connection is available
	//
	if (is_arriving) {
		authorized_text = 'Welcome ';
	} else {
		authorized_text = 'Goodbye ';
	}
	authorized_text += person.name;
	document.querySelector('.authorized-text').innerHTML = authorized_text;
}

function notAuthorized() {
	container.innerHTML = not_authorized_template.innerHTML;
	// need a way for user to return to numpad
	// also the numpad should automatically reappear after a short time
}