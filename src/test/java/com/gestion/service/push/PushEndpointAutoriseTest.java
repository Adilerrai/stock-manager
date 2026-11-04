package com.gestion.service.push;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PushEndpointAutoriseTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://fcm.googleapis.com/fcm/send/abc",
            "https://updates.push.services.mozilla.com/wpush/v2/abc",
            "https://web.push.apple.com/QGabc",
            "https://wns2-par02p.notify.windows.com/w/?token=abc"
    })
    void lesServicesDesNavigateursSontAcceptes(String endpoint) {
        assertTrue(PushNotificationService.endpointAutorise(endpoint));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "http://fcm.googleapis.com/fcm/send/abc",
            "https://169.254.169.254/latest/meta-data",
            "https://localhost:8009/api/admin",
            "https://pgsql.prod:5432/",
            "https://fcm.googleapis.com.attaquant.example/abc",
            "https://attaquantnotify.windows.com/abc",
            "https://fcm.googleapis.com:8443/abc",
            "https://user@fcm.googleapis.com/abc",
            "pas une url"
    })
    void toutAutreHoteEstRefuse(String endpoint) {
        assertFalse(PushNotificationService.endpointAutorise(endpoint));
    }
}
