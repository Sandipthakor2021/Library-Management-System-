package com.library.notification;

import com.library.domain.Patron;

/** Delivery channel abstraction (email, SMS, push...). */
public interface NotificationService {
    void notify(Patron patron, String message);
}
