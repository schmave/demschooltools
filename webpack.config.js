const path = require('path');
const webpack = require('webpack');

module.exports = {
    context: __dirname + '/app/assets/javascripts',
    entry: './main.js',
    output: {
        filename: './app/assets/javascripts/bundle.js'
    },
    resolve: {
        alias: {
            handlebars: "handlebars/dist/handlebars",
            jquery: "jquery/src/jquery"
        }
    },
    plugins: [
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery"
        })
    ]
};
