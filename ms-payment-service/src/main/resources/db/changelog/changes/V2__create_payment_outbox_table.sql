-- F-02 saga return path: transactional outbox for payment results.
-- Written in the same tx as the payment row; relayed to payment.events by PaymentOutboxRelay.
CREATE TABLE payment_outbox (
    id           VARCHAR(36)  NOT NULL,
    event_id     VARCHAR(36)  NOT NULL UNIQUE,
    aggregate_id VARCHAR(36),
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    attempts     INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    published_at TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_payment_outbox_status_created ON payment_outbox (status, created_at);
