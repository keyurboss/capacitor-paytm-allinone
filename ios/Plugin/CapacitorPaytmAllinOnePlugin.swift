import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CapacitorPaytmAllinOnePlugin)
public class CapacitorPaytmAllinOnePlugin: CAPPlugin {
    var call: CAPPluginCall?
    let handler = AIHandler()
    
    public override func load() {
        NotificationCenter.default.addObserver(self, selector: #selector(handleOpenURL(_:)), name: Notification.Name.capacitorOpenURL, object: nil)
    }
    
    @objc func startTransaction(_ call: CAPPluginCall) {
        self.call = call
        let mid = call.getString("mid") ?? ""
        let amount = call.getString("amount") ?? ""
        let orderId = call.getString("orderId") ?? ""
        let txnToken = call.getString("txnToken") ?? ""
        let isStaging = call.getBool("isStaging") ?? false
        var callbackUrl: String?
        if let callback = call.getString("callbackUrl"), !callback.isEmpty {
            callbackUrl = callback
        }
        let restrictAppInvoke = call.getBool("restrictAppInvoke") ?? false

        print("mid:", mid, ", amount:", amount, ", orderId:", orderId, ", txnToken:", txnToken, ", callbackUrl:", callbackUrl as Any, ", isStaging:", isStaging, ", restrictAppInvoke:", restrictAppInvoke)
        
        self.handler.setBridgeName(name: "IonicCapacitor")
        self.handler.restrictAppInvokeFlow(restrict: restrictAppInvoke)
        self.handler.openPaytm(merchantId: mid, orderId: orderId, txnToken: txnToken, amount: amount, callbackUrl: callbackUrl, delegate: self, environment: isStaging ? .staging : .production)
    }
    
    @objc func handleOpenURL(_ notification: Notification) {
        if let obj = notification.object as? [AnyHashable: Any], let url = obj["url"] as? URL {
            var dict = [String:Any]()
            let components = URLComponents(url: url, resolvingAgainstBaseURL: true)
            components?.queryItems?.forEach { dict[$0.name] = $0.value }
            
            if let response = dict["response"] as? String, let data = response.data(using: .utf8), let responseDict = try? JSONSerialization.jsonObject(with: data) as? [String:Any], let status = responseDict["STATUS"] as? String {
                if status == "TXN_SUCCESS" {
                    sendResponse(type: .success(dict))
                } else {
                    let message = responseDict["RESPMSG"] as? String
                    sendResponse(type: .error(message))
                }
            } else {
                let status = dict["status"] as? String
                if status == "PYTM_103" {
                    sendResponse(type: .success(dict))
                } else {
                    let message = self.statusReason(for: status ?? "")
                    sendResponse(type: .error(message))
                }
            }
        } else {
            sendResponse(type: .error("Callback not received."))
        }
    }
}

//MARK:- AIDelegate Methods
extension CapacitorPaytmAllinOnePlugin: AIDelegate {
    public func openPaymentWebVC(_ controller: UIViewController?) {
        if let vc = controller {
            DispatchQueue.main.async {[weak self] in
                self?.bridge?.viewController?.present(vc, animated: true, completion: nil)
            }
        } else {
            self.sendResponse(type: .error("Error loading Web Page."))
        }
    }
    
    public func didFinish(with status: AIPaymentStatus, response: [String : Any]) {
        switch status {
        case .success:
            self.sendResponse(type: .success(response))

        case .failed:
            let message = response["RESPMSG"] as? String
            self.sendResponse(type: .error(message))

        case .pending:
            self.sendResponse(type: .pending(response))
            
        default:
            break
        }
    }
}

//MARK:- Private Methods
private extension CapacitorPaytmAllinOnePlugin {
    enum ResponseType {
        case success(PluginCallResultData)
        case error(String?)
        case pending(PluginCallResultData)
    }
    
    func sendResponse(type: ResponseType) {
        switch type {
        case .success(let data):
            self.call?.resolve(data)
            
        case .error(let message):
            self.call?.reject(message ?? "")
            
        case .pending(let data):
            self.call?.resolve(data)
        }
    }
    
    func getStringFromDictionary(dictionary: [String: Any]) -> String? {
        guard let jsonData = try? JSONSerialization.data(withJSONObject: dictionary,options: .prettyPrinted) else {
            return nil
        }
        return String(data: jsonData, encoding: .ascii)
    }
        
    func statusReason(for code: String) -> String {
        switch code {
        case "PYTM_100":
            return "none"
            
        case "PYTM_101":
            return "initiated"
            
        case "PYTM_102":
            return "paymentMode"
            
        case "PYTM_103":
            return "paymentDeduction"
            
        case "PYTM_104":
            return "errorInParameter"
            
        case "PYTM_105":
            return "error"
            
        case "PYTM_106":
            return "cancel"
            
        default:
            return ""
        }
    }
}
