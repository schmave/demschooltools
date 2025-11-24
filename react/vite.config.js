import react from "@vitejs/plugin-react";
import { dirname, resolve } from "path";
import { visualizer } from "rollup-plugin-visualizer";
import { fileURLToPath } from "url";
import { defineConfig } from "vite";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const DEV_PORT = 8082;

export default ({ command, mode }) =>
  defineConfig({
    plugins: [react(), visualizer()],
    publicDir: false, // Django handles static files
    base: "/django-static",
    build: {
      outDir: resolve(__dirname, "..", "django", "static-vite"),
      emptyOutDir: true,
      rollupOptions: {
        input: {
          custodia: resolve(__dirname, "custodia", "js", "app.jsx"),
          custodia_css: resolve(__dirname, "custodia", "js", "cssonly.js"),
          reactapp:  resolve(__dirname, "index.jsx"),
        },
      },
      manifest: "manifest.json", // django-vite requires this
      sourcemap: true,
    },
    server: {
      port: DEV_PORT,
      host: "0.0.0.0",
      // This is needed for font-source files to serve properly
      origin: `http://localhost:${DEV_PORT}`,
    },
    resolve: {
      dedupe: ["react", "react-dom"],
    },
  });
