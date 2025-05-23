import Foundation
import UIKit
import SumUpSDK

@objc(SumpupTest)
class SumpupTest: CDVPlugin {

    // Response codes (matching Android implementation)
    private let LOGIN_ERROR = 100
    private let LOGIN_CANCELED = 101
    private let CHECK_FOR_LOGIN_STATUS_FAILED = 102
    private let LOGOUT_FAILED = 103
    private let FAILED_CLOSE_CARD_READER_CONN = 104
    private let CARDREADER_INSTANCE_NOT_DEFINED = 105
    private let STOP_CARD_READER_ERROR = 106
    private let SHOW_SETTINGS_FAILED = 107
    private let SETTINGS_DONE = 108
    private let PREPARE_PAYMENT_ERROR = 109
    private let CARDREADER_NOT_READY_TO_TRANSMIT = 110
    private let ERROR_PREPARING_CHECKOUT = 111
    private let AUTH_ERROR = 112
    private let NO_ACCESS_TOKEN = 113
    private let AUTH_SUCCESSFUL = 114
    private let CANT_PARSE_AMOUNT = 115
    private let CANT_PARSE_CURRENCY = 116
    private let PAYMENT_ERROR = 117
    private let NO_AFFILIATE_KEY = 118

    private var currentCallbackId: String?

    override func pluginInitialize() {
        super.pluginInitialize()

        // Initialize SumUp SDK
        if let apiKey = getApiKey() {
            SumUpSDK.setup(withAPIKey: apiKey)
        }
    }

    // MARK: - Plugin Methods

    @objc(login:)
    func login(command: CDVInvokedUrlCommand) {
        currentCallbackId = command.callbackId

        guard let params = command.arguments.first as? [String: Any] else {
            let result = createReturnObject(code: NO_AFFILIATE_KEY, message: "No parameters provided")
            sendPluginResult(status: CDVCommandStatus_ERROR, result: result)
            return
        }

        let affiliateKey = getApiKey() ?? params["affiliateKey"] as? String ?? ""

        guard !affiliateKey.isEmpty else {
            let result = createReturnObject(code: NO_AFFILIATE_KEY, message: "No affiliate key available")
            sendPluginResult(status: CDVCommandStatus_ERROR, result: result)
            return
        }

        DispatchQueue.main.async {
            if let accessToken = params["accessToken"] as? String, !accessToken.isEmpty {
                SumUpSDK.login(withToken: accessToken) { [weak self] (success, error) in
                    self?.handleLoginResult(success: success, error: error)
                }
            } else {
                SumUpSDK.presentLogin(from: self.viewController) { [weak self] (success, error) in
                    self?.handleLoginResult(success: success, error: error)
                }
            }
        }
    }

    @objc(auth:)
    func auth(command: CDVInvokedUrlCommand) {
        currentCallbackId = command.callbackId

        guard let accessToken = command.arguments.first as? String, !accessToken.isEmpty else {
            let result = createReturnObject(code: NO_ACCESS_TOKEN, message: "No access token provided")
            sendPluginResult(status: CDVCommandStatus_ERROR, result: result)
            return
        }

        DispatchQueue.main.async {
            SumUpSDK.login(withToken: accessToken) { [weak self] (success, error) in
                if success {
                    let result = self?.createReturnObject(code: self?.AUTH_SUCCESSFUL ?? 0, message: "Authentication successful")
                    self?.sendPluginResult(status: CDVCommandStatus_OK, result: result)
                } else {
                    let errorMessage = error?.localizedDescription ?? "Authentication failed"
                    let result = self?.createReturnObject(code: self?.AUTH_ERROR ?? 0, message: errorMessage)
                    self?.sendPluginResult(status: CDVCommandStatus_ERROR, result: result)
                }
            }
        }
    }

    @objc(getSettings:)
    func getSettings(command: CDVInvokedUrlCommand) {
        currentCallbackId = command.callbackId

        DispatchQueue.main.async {
            SumUpSDK.presentCheckoutPreferences(from: self.viewController) { [weak self] (success, error) in
                if success {
                    let result = self?.createReturnObject(code: 1, message: "Settings displayed successfully")
                    self?.sendPluginResult(status: CDVCommandStatus_OK, result: result)
                } else {
                    let errorMessage = error?.localizedDescription ?? "Failed to show settings"
                    let result = self?.createReturnObject(code: self?.SHOW_SETTINGS_FAILED ?? 0, message: errorMessage)
                    self?.sendPluginResult(status: CDVCommandStatus_ERROR, result: result)
                }
            }
        }
    }

    @objc(logout:)
    func logout(command: CDVInvokedUrlCommand) {
        currentCallbackId = command.callbackId

        do {
            try SumUpSDK.logout()
            let result = createReturnObject(code: 1, message: "Logout successful")
            sendPluginResult(status: CDVCommandStatus_OK, result: result)
        } catch {
            let result = createReturnObject(code: LOGOUT_FAILED, message: error.localizedDescription)
            sendPluginResult(status: CDVCommandStatus_ERROR, result: result)
        }
    }

    @objc(isLoggedIn:)
    func isLoggedIn(command: CDVInvokedUrlCommand) {
        currentCallbackId = command.callbackId

        DispatchQueue.main.async {
            let loggedIn = SumUpSDK.isLoggedIn

            var result: [String: Any] = [
                "code": 1,
                "isLoggedIn": loggedIn
            ]

            self.sendPluginResult(status: CDVCommandStatus_OK, result: result)
        }
    }

    @objc(prepare:)
    func prepare(command: CDVInvokedUrlCommand) {
        currentCallbackId = command.callbackId

        DispatchQueue.main.async {
            do {
                try SumUpSDK.prepareForCheckout()
                let result = self.createReturnObject(code: 1, message: "SumUp checkout prepared successfully")
                self.sendPluginResult(status: CDVCommandStatus_OK, result: result)
            } catch {
                let result = self.createReturnObject(code: self.ERROR_PREPARING_CHECKOUT, message: error.localizedDescription)
                self.sendPluginResult(status: CDVCommandStatus_ERROR, result: result)
            }
        }
    }

    @objc(closeConnection:)
    func closeConnection(command: CDVInvokedUrlCommand) {
        currentCallbackId = command.callbackId

        // In iOS SDK, connection is handled automatically
        let result = createReturnObject(code: 1, message: "Connection handling is automatic in iOS SDK")
        sendPluginResult(status: CDVCommandStatus_OK, result: result)
    }

    @objc(pay:)
    func pay(command: CDVInvokedUrlCommand) {
        currentCallbackId = command.callbackId

        guard command.arguments.count >= 1 else {
            let result = createReturnObject(code: CANT_PARSE_AMOUNT, message: "Amount parameter missing")
            sendPluginResult(status: CDVCommandStatus_ERROR, result: result)
            return
        }

        // Parse amount
        guard let amountString = command.arguments[0] as? String,
              let amount = Decimal(string: amountString) else {
            let result = createReturnObject(code: CANT_PARSE_AMOUNT, message: "Can't parse amount")
            sendPluginResult(status: CDVCommandStatus_ERROR, result: result)
            return
        }

        // Parse title (optional)
        let title = (command.arguments.count > 1) ? (command.arguments[1] as? String ?? "") : ""

        // Parse currency (optional)
        var currencyCode: SMPCurrencyCode = .EUR // Default
        if command.arguments.count > 2, let currencyString = command.arguments[2] as? String {
            currencyCode = parseCurrency(currencyString) ?? .EUR
        } else if SumUpSDK.isLoggedIn {
            // Use merchant's default currency if available
            currencyCode = SumUpSDK.currentMerchant?.currencyCode ?? .EUR
        }

        // Create checkout request
        let checkoutRequest = CheckoutRequest(total: amount, title: title, currencyCode: currencyCode)

        DispatchQueue.main.async {
            SumUpSDK.checkout(with: checkoutRequest, from: self.viewController) { [weak self] (result, error) in
                self?.handleCheckoutResult(result: result, error: error)
            }
        }
    }

    // MARK: - Helper Methods

    private func getApiKey() -> String? {
        return Bundle.main.object(forInfoDictionaryKey: "SUMUP_API_KEY") as? String
    }

    private func handleLoginResult(success: Bool, error: Error?) {
        if success {
            let result = createReturnObject(code: 1, message: "Login successful")
            sendPluginResult(status: CDVCommandStatus_OK, result: result)
        } else if let error = error {
            let result = createReturnObject(code: LOGIN_ERROR, message: error.localizedDescription)
            sendPluginResult(status: CDVCommandStatus_ERROR, result: result)
        } else {
            let result = createReturnObject(code: LOGIN_CANCELED, message: "Login canceled")
            sendPluginResult(status: CDVCommandStatus_ERROR, result: result)
        }
    }

    private func handleCheckoutResult(result: CheckoutResult?, error: Error?) {
        if let checkoutResult = result {
            var resultDict: [String: Any] = [:]

            // Add transaction info
            if let transactionInfo = checkoutResult.transactionInfo {
                resultDict["transaction_code"] = transactionInfo.transactionCode
                resultDict["merchant_code"] = checkoutResult.additionalInfo?["merchant_code"]
                resultDict["amount"] = transactionInfo.amount.description
                resultDict["tip_amount"] = transactionInfo.tipAmount?.description ?? "0"
                resultDict["vat_amount"] = transactionInfo.vatAmount?.description ?? "0"
                resultDict["currency"] = transactionInfo.currencyCode.rawValue
                resultDict["status"] = transactionInfo.status.rawValue
                resultDict["payment_type"] = transactionInfo.paymentType.rawValue
                resultDict["entry_mode"] = transactionInfo.entryMode.rawValue
                resultDict["installments"] = transactionInfo.installments

                // Card info
                if let card = transactionInfo.card {
                    resultDict["card_type"] = card.type.rawValue
                    resultDict["last_4_digits"] = card.last4Digits
                }
            }

            sendPluginResult(status: CDVCommandStatus_OK, result: resultDict)
        } else if let error = error {
            let errorResult = createReturnObject(code: PAYMENT_ERROR, message: error.localizedDescription)
            sendPluginResult(status: CDVCommandStatus_ERROR, result: errorResult)
        } else {
            let errorResult = createReturnObject(code: PAYMENT_ERROR, message: "Payment failed")
            sendPluginResult(status: CDVCommandStatus_ERROR, result: errorResult)
        }
    }

    private func parseCurrency(_ currencyString: String) -> SMPCurrencyCode? {
        switch currencyString.uppercased() {
        case "EUR": return .EUR
        case "USD": return .USD
        case "GBP": return .GBP
        case "CHF": return .CHF
        case "DKK": return .DKK
        case "NOK": return .NOK
        case "SEK": return .SEK
        case "PLN": return .PLN
        case "CZK": return .CZK
        case "HUF": return .HUF
        case "BGN": return .BGN
        case "RON": return .RON
        case "HRK": return .HRK
        case "BRL": return .BRL
        case "CLP": return .CLP
        case "COP": return .COP
        default: return nil
        }
    }

    private func createReturnObject(code: Int, message: String) -> [String: Any] {
        return [
            "code": code,
            "message": message
        ]
    }

    private func sendPluginResult(status: CDVCommandStatus, result: [String: Any]?) {
        guard let callbackId = currentCallbackId else { return }

        let pluginResult: CDVPluginResult
        if let result = result {
            pluginResult = CDVPluginResult(status: status, messageAs: result)
        } else {
            pluginResult = CDVPluginResult(status: status)
        }

        pluginResult?.setKeepCallbackAs(false)
        commandDelegate?.send(pluginResult, callbackId: callbackId)
        currentCallbackId = nil
    }
}
