// https://codelabs.developers.google.com/codelabs/your-first-pwapp/#4

// update cache names any time any of the cached files change
const CACHE_NAME = 'static-cache-v255';

const FILES_TO_CACHE = [
	'/assets/checkin/app.html',
	'/assets/checkin/app-compiled.js',
	'/assets/checkin/icon-192.png',
	'/assets/checkin/icon-512.png',
	'/assets/checkin/manifest.json',
	'/assets/checkin/localforage.js',
	'/assets/checkin/style.css'
];

self.addEventListener('install', (evt) => {
	console.log('[ServiceWorker] Install');
	evt.waitUntil(
		caches.open(CACHE_NAME).then((cache) => {
			console.log('[ServiceWorker] Pre-caching offline page');
			return cache.addAll(FILES_TO_CACHE);
		})
	);
 	self.skipWaiting();
});

self.addEventListener('activate', (evt) => {
	console.log('[ServiceWorker] Activate');
	// Remove previous cached data from disk.
	evt.waitUntil(
	    caches.keys().then((keyList) => {
	      return Promise.all(keyList.map((key) => {
	        if (key !== CACHE_NAME) {
	          console.log('[ServiceWorker] Removing old cache', key);
	          return caches.delete(key);
	        }
	      }));
	    })
	);
	self.clients.claim();
});

self.addEventListener('fetch', (evt) => {
	console.log('[ServiceWorker] Fetch', evt.request.url);
	// don't cache application data or logging in
	if (evt.request.url.includes('/data') || evt.request.url.includes('/login')) {
		return;
	}
	// replace the default fetch handler with our custom handler that uses the cache
	evt.respondWith(fetchResources(evt));
});

async function fetchResources(evt) {
	// first try to get resources from the server, falling back on the cache if the server cannot be reached
	try {
		const response = await fetch(evt.request);
		if (response.ok) return response;
	} catch (err) {
		console.error(err);
	}
	return cache.match(evt.request);
}
