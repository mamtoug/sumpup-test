import Foundation

@objc(SumpupTest)
class SumpupTest: CDVPlugin {

    @objc(showMessage:)
    func showMessage(command: CDVInvokedUrlCommand) {
        let message = command.arguments[0] as? String ?? "Hello from SumpupTest!"

        DispatchQueue.main.async {
            let alert = UIAlertController(title: "SumpupTest", message: message, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default))
            self.viewController.present(alert, animated: true)
        }

        let result = [
            "message": message,
            "status": "success",
            "platform": "iOS"
        ]

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(getDeviceInfo:)
    func getDeviceInfo(command: CDVInvokedUrlCommand) {
        let deviceInfo = [
            "platform": "iOS",
            "model": UIDevice.current.model,
            "name": UIDevice.current.name,
            "version": UIDevice.current.systemVersion,
            "identifier": UIDevice.current.identifierForVendor?.uuidString ?? "unknown"
        ]

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: deviceInfo)
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(initSumUp:)
    func initSumUp(command: CDVInvokedUrlCommand) {
        let apiKey = command.arguments[0] as? String

        print("Initializing SumUp with API key: \(apiKey != nil ? "***" : "nil")")

        // This is a placeholder for actual SumUp SDK initialization
        let result = [
            "status": "initialized",
            "message": "SumUp SDK initialized (simulated)",
            "apiKey": apiKey != nil ? "provided" : "missing"
        ]

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(testPayment:)
    func testPayment(command: CDVInvokedUrlCommand) {
        let amount = command.arguments[0] as? Double ?? 0.0
        let currency = command.arguments[1] as? String ?? "EUR"

        print("Processing test payment: \(amount) \(currency)")

        // This is a placeholder for actual SumUp payment processing
        let result = [
            "status": "success",
            "message": "Payment processed (simulated)",
            "amount": amount,
            "currency": currency,
            "transactionId": "TEST_\(Int(Date().timeIntervalSince1970))",
            "timestamp": Int(Date().timeIntervalSince1970)
        ] as [String : Any]

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }
}
