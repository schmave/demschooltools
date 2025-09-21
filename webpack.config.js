const path = require('path');
const webpack = require('webpack');

const ESLintPlugin = require('eslint-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ReactRefreshWebpackPlugin = require('@pmmmwh/react-refresh-webpack-plugin');

module.exports = function (env, argv) {
  const isDev = argv.mode !== 'production';
    return {
        devtool: isDev ? 'eval-source-map' : 'source-map',
        context: path.join(__dirname, '/app/assets'),
        entry: {
            bundle: './javascripts/main.js',
            checkin: './checkin/app.js',
            reactapp: '../react_app/index.js',
        },
        module: {
            rules: [
                {
                    test: /\.s[ac]ss$/i,
                    exclude: /node_modules/,
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
                        plugins: [
                          isDev && require.resolve('react-refresh/babel'),
                        ].filter(Boolean),
                      },
                    },
                  ],
                },
            ],
        },
        output: {
            path: path.resolve(__dirname, 'app/assets/javascripts/gen'),
            clean: true,
            filename: '[name].js',
            sourceMapFilename: '[file].map[query]',
             publicPath: isDev ? '/' : undefined,
        },
        resolve: {
            extensions: [".js", ".jsx"],
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
                hash: argv.mode != 'production',
                chunks: ['checkin'],
                templateParameters: {
                    rollbarEnvironment:
                        argv.mode == 'production'
                            ? 'production'
                            : 'development',
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
              static: false, // we don't serve files from disk
              devMiddleware: {
                publicPath: '/', // matches output.publicPath
                writeToDisk: false,
              },
              historyApiFallback: true,
            }
          : undefined,
    };
};
