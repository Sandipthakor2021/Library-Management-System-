package com.library.notification;

import com.library.domain.Patron;

import java.util.logging.Logger;

/** Stand-in for a real email/SMS gateway: writes the message to the log. */
public class ConsoleNotificationService implements NotificationService {
    private static final Logger LOG = Logger.getLogger(ConsoleNotificationService.class.getName());

    @Override
    public void notify(Patron patron, String message) {
        LOG.info("NOTIFY " + patron.getName() + " <" + patron.getEmail() + ">: " + message);
    }
}
