var exec = require('cordova/exec');

exports.start = function(success, error) {
    exec(success, error, 'MyNfcPlugin', 'start', []);
};

exports.stop = function(success, error) {
    exec(success, error, 'MyNfcPlugin', 'stop', []);
};