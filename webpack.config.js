const path = require('path');
const webpack = require('webpack');

module.exports = function(env) {
    const isDebug = env && (env.debug == 'true');

    return {
        context: path.join(__dirname, '/app/assets/javascripts'),
        entry: './main.js',
        output: {
            filename: './app/assets/javascripts/gen/bundle.js',
        },
        resolve: {
            alias: {
                handlebars: "handlebars/dist/handlebars",
                jquery: "jquery/src/jquery"
            }
        },
         module: {
            rules: [
              {
                test: /\.js$/,
                exclude: /node_modules/,
                loader: "eslint-loader",
              },
            ],
          },
          plugins: [
            new webpack.ProvidePlugin({
                $: "jquery",
                jQuery: "jquery"
            }),
            // Production-only plugins go below this line
        ].concat(isDebug ? [] : [
            new webpack.optimize.UglifyJsPlugin({
                compress: {
                    warnings: false,
                }
            })
        ])
    }
};
