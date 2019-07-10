
if ('serviceWorker' in navigator) {
  	window.addEventListener('load', () => {
	    navigator.serviceWorker.register('/assets/checkin/service-worker.js')
	        .then((reg) => {
	          	console.log('Service worker registered.', reg);
	        });
  	});
}

const data = [
	{ id: 1, name: 'Sam', code: '5336', records_today: 0 },
	{ id: 2, name: 'Lucy', code: '2969', records_today: 0 },
	{ id: 3, name: 'Robin', code: '4718', records_today: 0 }
];

// all codes are constrained to be the same length
const code_length = data[0].code.length;

var code_entered;

const container = document.querySelector('#container');
const numpad_template = document.querySelector('#numpad-template');
const authorized_template = document.querySelector('#authorized-template');
const not_authorized_template = document.querySelector('#not-authorized-template');

resetApp();

document.querySelectorAll(".checkin-app-number-button").forEach(function(button) {
	button.addEventListener("click", function() {
		addNumberToCodeEntered(this.dataset.number);
	});
});

function resetApp() {
	code_entered = '';
	container.innerHTML = numpad_template.innerHTML;
}

function addNumberToCodeEntered(number) {
	code_entered += String(number);
	console.log(code_entered);
	if (code_entered.length === code_length) {
		checkCode();
	}
}

function checkCode() {
	for (let i = 0; i < data.length; i++) {
		let person = data[i];
		if (code_entered === person.code) {
			authorized(person);
			return;
		}
	}
	notAuthorized();
}

function authorized(person) {
	container.innerHTML = authorized_template.innerHTML;
	var authorized_text = '';
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