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
    publicDir: false, // Django handles static files
    base: "/django-static",
    build: {
      outDir: resolve(__dirname, "..", "django", "static-vite"),
      emptyOutDir: true,
      rollupOptions: {
        input: {
          custodia: resolve(__dirname, "src", "js", "app.jsx"),
          custodia_css: resolve(__dirname, "src", "js", "cssonly.js"),
        },
      },
      manifest: "manifest.json", // django-vite requires this
      sourcemap: true,
    },
    server: {
      port: 8082,
      host: "0.0.0.0",
    },
    resolve: {
      dedupe: ["react", "react-dom"],
    },
  });
