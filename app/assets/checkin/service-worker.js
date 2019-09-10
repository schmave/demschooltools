// https://codelabs.developers.google.com/codelabs/your-first-pwapp/#4

// update cache names any time any of the cached files change
const CACHE_NAME = 'static-cache-v189';

const FILES_TO_CACHE = [
	'/assets/checkin/app.html',
	'/assets/checkin/app.js',
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
	// Use cache-first strategy. Only request resources from the server if they are not found in the cache.
	evt.respondWith(
	    caches.open(CACHE_NAME).then((cache) => {
	      return cache.match(evt.request)
			.then((response) => {
			  return response || fetch(evt.request);
			});
	    })
	);
});
