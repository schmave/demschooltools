import ReactRefreshWebpackPlugin from "@pmmmwh/react-refresh-webpack-plugin";
import ESLintPlugin from "eslint-webpack-plugin";
import { resolve as _resolve, dirname, join } from "path";
import { fileURLToPath } from "url";

const DEV_SERVER_PORT = 8082;

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default function (env, argv) {
  const isDevelopment = argv.mode !== "production";
  return {
    mode: isDevelopment ? "development" : "production",
    devtool: "source-map",
    context: join(__dirname, "/src"),
    entry: {
      custodia: "./js/app.jsx",
    },
    output: {
      path: _resolve(__dirname, "..", "django", "static", "js"),
    },
    devServer: {
      client: {
        logging: "verbose",
        overlay: true,
      },
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, PATCH, OPTIONS",
        "Access-Control-Allow-Headers": "X-Requested-With, content-type, Authorization",
      },
      host: "0.0.0.0",
      hot: true,
      port: DEV_SERVER_PORT,
    },
    module: {
      rules: [
        {
          test: /\.css$/i,
          use: [
            // Creates `style` nodes from JS strings
            "style-loader",
            // Translates CSS into CommonJS
            "css-loader",
          ],
        },
        {
          test: /\.s[ac]ss$/i,
          use: [
            // Creates `style` nodes from JS strings
            "style-loader",
            // Translates CSS into CommonJS
            "css-loader",
            // Compiles Sass to CSS
            "sass-loader",
          ],
        },
        {
          test: /\.(jsx|js|mjs|cjs)$/,
          exclude: /node_modules/,
          use: [
            {
              loader: "babel-loader",
              options: {
                plugins: [isDevelopment && "react-refresh/babel"].filter(
                  Boolean,
                ),
                presets: [
                  [
                    "@babel/preset-env",
                    {
                      targets: {
                        chrome: 58,
                        edge: 16,
                        firefox: 57,
                        safari: 11,
                      },
                    },
                  ],
                  ["@babel/preset-react", { runtime: "automatic", development: isDevelopment }],
                ],
              },
            },
          ],
        },
      ],
    },
    resolve: {
      extensions: [".js", ".jsx"],
      fallback: {
        path: false,
        fs: false,
      },
    },
    plugins: [
      new ESLintPlugin({
        extensions: ["js", "jsx"],
        context: join(__dirname, "/src"),
        failOnError: true, // This makes webpack fail on ESLint errors
        failOnWarning: false, // Set to true if you want warnings to fail the build too
      }),
      isDevelopment &&
        new ReactRefreshWebpackPlugin({
          esModule: true,
          exclude: /node_modules/,
        }),
    ].filter(Boolean),
  };
}
