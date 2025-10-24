import react from "@vitejs/plugin-react";
import { dirname, resolve } from "path";
import { visualizer } from "rollup-plugin-visualizer";
import { fileURLToPath } from "url";
import { defineConfig } from "vite";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default ({ command, mode }) =>
  defineConfig({
    plugins: [react(), visualizer()],
    root: ".",
    publicDir: false, // Django handles static files
    base: "/django-static",
    build: {
      outDir: resolve(__dirname, "..", "django", "static-vite"),
      emptyOutDir: true,
      rollupOptions: {
        input: {
          custodia: resolve(__dirname, "src", "js", "app.jsx"),
        },
      },
      manifest: "manifest.json", // django-vite requires this
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
