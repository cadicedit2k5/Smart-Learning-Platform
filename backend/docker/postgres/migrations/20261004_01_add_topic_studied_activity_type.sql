ALTER TABLE core.learning_activity
DROP CONSTRAINT IF EXISTS learning_activity_event_type_check;

ALTER TABLE core.learning_activity
    ADD CONSTRAINT learning_activity_event_type_check
        CHECK (
            event_type IN (
                           'TOPIC_STARTED',
                           'TOPIC_STUDIED',
                           'TOPIC_COMPLETED',
                           'ASSIGNMENT_SUBMITTED',
                           'ASSIGNMENT_GRADED'
                )
            );