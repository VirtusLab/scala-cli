package scala.build.bsp

/**
 * Response error codes as defined in JSON RPC.
 * [[https://www.jsonrpc.org/specification#error_object]]
 */
object JsonRpcErrorCodes {
  val ParseError: Int     = -32700 // Invalid JSON was received by the server.
  val InvalidRequest: Int = -32600 // The JSON sent is not a valid Request object.
  val MethodNotFound: Int = -32601 // The method does not exist / is not available.
  val InvalidParams: Int  = -32602 // Invalid method parameter(s).
  val InternalError: Int  = -32603 // Internal JSON-RPC error.
}
