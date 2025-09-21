const path = require('path');
const webpack = require('webpack');

const ESLintPlugin = require('eslint-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = function (env, argv) {
    return {
        devtool: 'source-map',
        context: path.join(__dirname, '/app/assets'),
        entry: {
            bundle: './javascripts/main.js',
            checkin: './checkin/app.js',
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
        },
        resolve: {
            extensions: ["", ".js", ".jsx"],
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
        ],
    };
};
