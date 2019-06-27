CREATE TYPE alert_definition_type AS (
    id                  int,
    name                varchar(256),
    description         text,
    team                varchar(256),
    responsible_team    varchar(256),
    entities            hstore[],
    entities_exclude    hstore[],
    condition           text,
    notifications       text[],
    check_definition_id int,
    status              zzm_data.definition_status,
    priority            int,
    last_modified       timestamptz,
    last_modified_by    text,
    period              text,
    template            boolean,
    parent_id           int,
    parameters          hstore,
    tags                text[],
    false_positive_rate real
);
