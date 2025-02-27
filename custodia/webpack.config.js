const path = require("path");

const HtmlWebpackPlugin = require("html-webpack-plugin");
const ReactRefreshWebpackPlugin = require("@pmmmwh/react-refresh-webpack-plugin");

const DEV_SERVER_PORT = 8081;

module.exports = function (env, argv) {
  const isDevelopment = argv.mode !== "production";
  return {
    mode: isDevelopment ? "development" : "production",
    devtool: "source-map",
    context: path.join(__dirname, "/src"),
    entry: {
      app: "./js/app.jsx",
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
                plugins: [isDevelopment && require.resolve("react-refresh/babel")].filter(Boolean),
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
                  "@babel/preset-react",
                ],
              },
            },
          ],
        },
      ],
    },
    output: {
      filename: isDevelopment ? "[name].js" : "[name].[chunkhash].js",
      sourceMapFilename: "[file].map",
    },
    resolve: {
      extensions: ["", ".js", ".jsx"],
      fallback: {
        path: false,
        fs: false,
      },
    },
    plugins: [
      isDevelopment &&
        new ReactRefreshWebpackPlugin({
          overlay: {
            sockPort: DEV_SERVER_PORT,
          },
        }),

      new HtmlWebpackPlugin({
        filename: "index.html",
        template: "../../django/custodia/templates/index.html",
        hash: argv.mode != "production",
        chunks: ["app"],
        publicPath: "/django-static/js/",
        scriptLoading: "blocking",
        templateParameters: {
          rollbarEnvironment: argv.mode == "production" ? "production" : "development",
        },
      }),
    ].filter(Boolean),
  };
};
