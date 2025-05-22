var exec = require('cordova/exec');

/**
 * SumpupTest Plugin
 * A Cordova plugin for SumUp payment integration testing
 */
var SumpupTest = {

    /**
     * Show a test message
     * @param {string} message - The message to display
     * @param {function} success - Success callback
     * @param {function} error - Error callback
     */
    showMessage: function(message, success, error) {
        exec(success, error, 'SumpupTest', 'showMessage', [message || 'Hello from SumpupTest!']);
    },

    /**
     * Get device information
     * @param {function} success - Success callback
     * @param {function} error - Error callback
     */
    getDeviceInfo: function(success, error) {
        exec(success, error, 'SumpupTest', 'getDeviceInfo', []);
    },

    /**
     * Test SumUp initialization (placeholder)
     * @param {string} apiKey - SumUp API key
     * @param {function} success - Success callback
     * @param {function} error - Error callback
     */
    initSumUp: function(apiKey, success, error) {
        exec(success, error, 'SumpupTest', 'initSumUp', [apiKey]);
    },

    /**
     * Test payment simulation (placeholder)
     * @param {number} amount - Payment amount
     * @param {string} currency - Currency code
     * @param {function} success - Success callback
     * @param {function} error - Error callback
     */
    testPayment: function(amount, currency, success, error) {
        exec(success, error, 'SumpupTest', 'testPayment', [amount, currency || 'EUR']);
    }
};

module.exports = SumpupTest;
