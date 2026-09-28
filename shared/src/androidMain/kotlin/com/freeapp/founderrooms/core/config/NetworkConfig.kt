package com.freeapp.founderrooms.core.config

actual object NetworkConfig {
    // Preview tunnel in front of the systemd unit founderrooms-server on the
    // VPS. Temporary by nature: if the tunnel restarts the hostname changes
    // and the APK must be rebuilt against the new one.
    actual val BASE_URL: String = "https://remain-immunology-festival-attribute.trycloudflare.com"
}
