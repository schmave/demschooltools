const path = require('path');
const webpack = require('webpack');
const ESLintPlugin = require('eslint-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');

// Use ESBUILD by running: BUNDLER=esbuild npm run compile
const USE_ESBUILD = (process.env.BUNDLER || '').toLowerCase() === 'esbuild';

// Optional (only required when using esbuild):
let EsbuildPlugin = null;
if (USE_ESBUILD) {
  try {
    ({ EsbuildPlugin } = require('esbuild-loader'));
  } catch {
    // falls back to Babel if esbuild-loader isn't installed
    EsbuildPlugin = null;
  }
}

const ESBUILD_TARGET = ['chrome58', 'edge16', 'firefox57', 'safari11'];

module.exports = (env, argv) => {
  const isProd = argv.mode === 'production';

  /** JS/TS rule builder */
  const jsRule = USE_ESBUILD && EsbuildPlugin
    ? {
        test: /\.(jsx?|mjs|cjs)$/,
        exclude: /node_modules/,
        loader: 'esbuild-loader',
        options: { target: ESBUILD_TARGET, loader: 'jsx' },
      }
    : {
        test: /\.(jsx?|mjs|cjs)$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: [
              [
                '@babel/preset-env',
                {
                  targets: { chrome: 58, edge: 16, firefox: 57, safari: 11 },
                  modules: false,
                  bugfixes: true,
                  useBuiltIns: 'entry',
                  corejs: 3,
                },
              ],
              '@babel/preset-react',
            ],
          },
        },
      };

  return {
    context: path.join(__dirname, 'app'),
    entry: {
      bundle: './assets/javascripts/main.js',
      checkin: './assets/checkin/app.js',
      reactapp: './react_app/index.js',
    },
    output: {
      path: path.resolve(__dirname, 'app/assets/javascripts/gen'),
      filename: '[name].js',
      clean: true,
      sourceMapFilename: '[file].map[query]',
      assetModuleFilename: 'assets/[name][ext][query]',
    },
    devtool: isProd ? 'source-map' : 'eval-cheap-module-source-map',
    cache: { type: 'filesystem' },
    module: {
      rules: [
        // JS/JSX via esbuild or Babel
        jsRule,

        // SCSS/CSS
        {
          test: /\.s?css$/i,
          use: [
            'style-loader',
            { loader: 'css-loader', options: { importLoaders: 1 } },
            'sass-loader',
          ],
        },

        // Images
        {
          test: /\.(png|jpe?g|gif|svg)$/i,
          type: 'asset/resource',
        },

        // Fonts / other assets
        {
          test: /\.(woff2?|eot|ttf|otf)$/i,
          type: 'asset/resource',
        },
      ],
    },
    resolve: {
      extensions: ['.js', '.jsx', '.mjs', '.cjs', '.json'],
      alias: {
        handlebars: 'handlebars/dist/handlebars',
        jquery: 'jquery/src/jquery',
      },
    },
    optimization: {
      splitChunks: { chunks: 'all' },
      runtimeChunk: 'single',
      minimize: isProd && !!EsbuildPlugin && USE_ESBUILD,
      minimizer:
        isProd && USE_ESBUILD && EsbuildPlugin
          ? [new EsbuildPlugin({ target: ESBUILD_TARGET })]
          : undefined, // falls back to Terser when not using esbuild
    },
    plugins: [
      new ESLintPlugin({
        fix: true,
        context: __dirname,
        extensions: ['js','jsx','mjs','cjs'],
        overrideConfigFile: path.resolve(__dirname, '.eslintrc.json'),
        failOnError: false,       // optional while stabilizing
        emitWarning: true,
        lintDirtyModulesOnly: true
      }),
      new webpack.ProvidePlugin({ $: 'jquery', jQuery: 'jquery' }),
      new HtmlWebpackPlugin({
        filename: '../../checkin/app.html',
        template: 'assets/checkin/template.html',
        hash: !isProd,
        chunks: ['checkin'],
        templateParameters: {
          rollbarEnvironment: isProd ? 'production' : 'development',
        },
      }),
      new webpack.DefinePlugin({
        'process.env.NODE_ENV': JSON.stringify(isProd ? 'production' : 'development'),
      }),
    ],
    performance: { hints: false },
    stats: 'errors-warnings',
  };
};
