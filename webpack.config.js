const path = require("path");
const webpack = require("webpack");

const ESLintPlugin = require("eslint-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const { EsbuildPlugin } = require("esbuild-loader");

const ESBUILD_TARGET = ["chrome58", "edge16", "firefox57", "safari11"];

module.exports = function (env, argv) {
  return {
    devtool: "source-map",
    context: path.join(__dirname, "/app/assets"),
    entry: {
      bundle: "./javascripts/main.js",
      checkin: "./checkin/app.js",
    },
    module: {
      rules: [
        {
          test: /\.(?:js|mjs|cjs)$/,
          exclude: /node_modules/,
          use: {
            loader: "esbuild-loader",
            options: {
              target: ESBUILD_TARGET,
            },
          },
        },
      ],
    },
    output: {
      path: path.resolve(__dirname, "app/assets/javascripts/gen"),
      filename: "[name].js",
      sourceMapFilename: "[file].[chunkhash].map[query]",
    },
    resolve: {
      alias: {
        handlebars: "handlebars/dist/handlebars",
        jquery: "jquery/src/jquery",
      },
    },
    optimization: {
      minimize: true,
      minimizer: [
        new EsbuildPlugin({
          target: ESBUILD_TARGET,
        }),
      ],
    },

    plugins: [
      new ESLintPlugin({
        fix: true,
        useEslintrc: true,
      }),
      new webpack.ProvidePlugin({
        $: "jquery",
        jQuery: "jquery",
      }),
      new HtmlWebpackPlugin({
        filename: "../../checkin/app.html",
        template: "checkin/template.html",
        hash: true,
        chunks: ["checkin"],
        templateParameters: {
          rollbarEnvironment: argv.mode == "production" ? "production" : "dev",
        },
      }),
    ],
  };
};
