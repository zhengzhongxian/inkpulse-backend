package com.inkpulse.service.outbox;

import com.inkpulse.corehelpers.JsonHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

        private final JdbcTemplate jdbcTemplate;

        /**
         * Publishes a message to the MassTransit EF Core outbox tables.
         * The Bus Outbox delivery service in the C# worker will sweep these
         * records and deliver them to RabbitMQ.
         *
         * @param queueName   RabbitMQ queue name (e.g. "sync-author-queue")
         * @param message     The message payload object to serialize
         * @param messageType MassTransit URN type (e.g. "urn:message:Namespace:Type")
         */
        @Transactional
        public void publish(String queueName, Object message, String messageType) {
                UUID outboxId = UUID.randomUUID();
                UUID messageId = UUID.randomUUID();
                UUID lockId = new UUID(0L, 0L);
                Timestamp now = Timestamp.from(Instant.now());

                String insertStateSql = "INSERT INTO \"OutboxState\" (\"OutboxId\", \"LockId\", \"Created\", \"Delivered\", \"LastSequenceNumber\") VALUES (?, ?, ?, NULL, NULL)";
                jdbcTemplate.update(insertStateSql, outboxId, lockId, now);

                String destinationAddress = "queue:" + queueName;

                // Wrap in MassTransit message envelope to match C# worker deserialization expectations
                Map<String, Object> envelope = new HashMap<>();
                envelope.put("messageId", messageId.toString());
                envelope.put("destinationAddress", destinationAddress);
                envelope.put("messageType", List.of(messageType));
                envelope.put("message", message);
                envelope.put("sentTime", now.toInstant().toString());

                String bodyJson = JsonHelper.serializeSafe(envelope);

                String messageTypeJson = JsonHelper.serializeSafe(List.of(messageType));

                String insertMessageSql = "INSERT INTO \"OutboxMessage\" (" +
                                "\"OutboxId\", \"MessageId\", \"ContentType\", \"MessageType\", \"Body\", " +
                                "\"SentTime\", \"DestinationAddress\", \"EnqueueTime\", \"ExpirationTime\", " +
                                "\"ConversationId\", \"CorrelationId\", \"InitiatorId\", \"RequestId\", " +
                                "\"Headers\", \"Properties\", \"InboxMessageId\", \"InboxConsumerId\", " +
                                "\"SourceAddress\", \"ResponseAddress\", \"FaultAddress\"" +
                                ") VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)";

                String contentType = "application/json";

                jdbcTemplate.update(insertMessageSql,
                                outboxId,
                                messageId,
                                contentType,
                                messageTypeJson,
                                bodyJson,
                                now,
                                destinationAddress);

                log.info("Published outbox message. OutboxId: {}, MessageId: {}, Destination: {}, Type: {}",
                                outboxId, messageId, destinationAddress, messageType);
        }
}
