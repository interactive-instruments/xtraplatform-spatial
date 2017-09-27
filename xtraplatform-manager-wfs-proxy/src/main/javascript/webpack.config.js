const resolve = require('path').resolve;
const webpackMerge = require('webpack-merge');

module.exports = function(env) {
//console.log(env)
const config = require('xtraplatform-manager/webpack.config.' + (env || 'development'));

const newConfig = webpackMerge(config(env), {
    context: resolve(__dirname)
})
//console.log(newConfig)
return newConfig

}

