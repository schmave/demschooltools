const path = require('path');
const webpack = require('webpack');

const ESLintPlugin = require('eslint-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ReactRefreshWebpackPlugin = require('@pmmmwh/react-refresh-webpack-plugin');

const makeCssRule = () => ({
  test: /\.css$/i,
  use: ['style-loader', 'css-loader'],
});

const makeScssRule = () => ({
  test: /\.s[ac]ss$/i,
  exclude: /node_modules/,
  use: ['style-loader', 'css-loader', 'sass-loader'],
});

const makeBabelRule = (isDev) => ({
  test: /\.(jsx|js|mjs|cjs)$/,
  exclude: /node_modules/,
  use: [
    {
      loader: 'babel-loader',
      options: {
        presets: [
          [
            '@babel/preset-env',
            {
              targets: {
                chrome: 58,
                edge: 16,
                firefox: 57,
                safari: 11,
              },
            },
          ],
          '@babel/preset-react',
        ],
        plugins: [
          require.resolve('@babel/plugin-proposal-class-properties'),
          isDev && require.resolve('react-refresh/babel'),
        ].filter(Boolean),
      },
    },
  ],
});

const makeBaseConfig = (isDev) => ({
  devtool: isDev ? 'eval-source-map' : 'source-map',
  module: {
    rules: [makeCssRule(), makeScssRule(), makeBabelRule(isDev)],
  },
});

module.exports = function (env, argv) {
  const isDev = argv.mode !== 'production';

  return {
    ...makeBaseConfig(isDev),
    context: path.join(__dirname, '/app/assets'),
    entry: {
      bundle: './javascripts/main.js',
      checkin: './checkin/app.js',
      reactapp: '../react_app/index.js',
      custodia: '../react_app/custodia/entry.js',
    },
    output: {
      path: path.resolve(__dirname, 'app/assets/javascripts/gen'),
      clean: true,
      filename: '[name].js',
      sourceMapFilename: '[file].map[query]',
      publicPath: isDev ? '/' : undefined,
    },
    resolve: {
      extensions: ['.js', '.jsx'],
      alias: {
        handlebars: 'handlebars/dist/handlebars',
        jquery: 'jquery/src/jquery',
      },
    },
    plugins: [
      new ESLintPlugin({
        fix: true,
        useEslintrc: true,
      }),
      new webpack.ProvidePlugin({
        $: 'jquery',
        jQuery: 'jquery',
      }),
      new HtmlWebpackPlugin({
        filename: '../../checkin/app.html',
        template: 'checkin/template.html',
        hash: isDev,
        chunks: ['checkin'],
        templateParameters: {
          rollbarEnvironment: isDev ? 'development' : 'production',
        },
      }),
      isDev && new ReactRefreshWebpackPlugin(),
    ].filter(Boolean),
    devServer: isDev
      ? {
          port: 8081,
          host: 'localhost',
          hot: true,
          allowedHosts: 'all',
          headers: { 'Access-Control-Allow-Origin': '*' },
          client: { overlay: true, progress: true },
          static: false,
          devMiddleware: {
            publicPath: '/',
            writeToDisk: false,
          },
          historyApiFallback: true,
        }
      : undefined,
  };
};
