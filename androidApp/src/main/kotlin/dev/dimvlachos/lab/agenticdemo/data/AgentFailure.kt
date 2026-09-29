package dev.dimvlachos.lab.agenticdemo.data

/** Why a turn failed, in terms the UI can act on, whichever provider's SDK raised it. */
class AgentFailure(val reason: Reason, cause: Throwable) : Exception(cause.message, cause) {
    enum class Reason {
        /** No connection, a timeout, or the provider's own outage: worth retrying. */
        Network,
        /** The key is missing, wrong, revoked or lacks access: retrying cannot help. */
        Auth,
        /** Too many requests or no quota left. */
        RateLimited,
        /** The request itself was rejected, e.g. an unknown model name. */
        Unavailable,
    }
}
