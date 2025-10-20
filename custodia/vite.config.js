import react from "@vitejs/plugin-react";
import { dirname, resolve } from "path";
import { fileURLToPath } from "url";
import { defineConfig } from "vite";
import { visualizer } from "rollup-plugin-visualizer";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default defineConfig({
  plugins: [react(), visualizer()],
  root: ".",
  publicDir: false, // Don't copy public directory since Django handles static files
  build: {
    outDir: resolve(__dirname, "..", "django", "static", "js"),
    emptyOutDir: false,
    rollupOptions: {
      input: {
        main: resolve(__dirname, "index.html"),
      },
    },
    manifest: true,
    sourcemap: true,
  },
  server: {
    port: 8082,
    host: "0.0.0.0",
    cors: true,
    headers: {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, PATCH, OPTIONS",
      "Access-Control-Allow-Headers": "X-Requested-With, content-type, Authorization",
    },
  },
  resolve: {
    extensions: [".js", ".jsx"],
  },
});
