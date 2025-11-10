import { copyFileSync, existsSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";

const filesToCopy = ["custodia.js", "custodia.js.map", "custodia.js.LICENSE.txt"];
const sourceDir = resolve("app/assets/javascripts/gen");
const targetDir = resolve("django/static/js");

const ensureDir = (dir) => {
  if (!existsSync(dir)) {
    mkdirSync(dir, { recursive: true });
  }
};

ensureDir(targetDir);

filesToCopy.forEach((filename) => {
  const source = resolve(sourceDir, filename);
  const destination = resolve(targetDir, filename);
  try {
    copyFileSync(source, destination);
    console.log(`Copied ${filename} to django/static/js`);
  } catch (error) {
    console.warn(`Failed to copy ${filename}: ${error.message}`);
  }
});
